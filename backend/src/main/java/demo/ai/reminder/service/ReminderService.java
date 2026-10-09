package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.RepeatRule;
import demo.ai.reminder.dto.DeletedCountResponse;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderResponse;
import demo.ai.reminder.dto.ReminderUpdateRequest;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderService {

    private static final Sort DEFAULT_SORT = Sort.by("id");
    private static final Sort LIST_SORT = Sort.by("sortOrder", "id");
    private static final Sort DUE_DATE_SORT = Sort.by("dueAt", "id");
    private static final Sort COMPLETED_SORT = Sort.by(Sort.Order.desc("completedAt"), Sort.Order.desc("id"));

    private final ReminderRepository reminderRepository;
    private final ListAccess listAccess;
    private final TagService tagService;
    private final CurrentUser currentUser;

    // 리스트를 지정하면 최상위 리마인더만 표시 순서대로(하위 작업은 각 부모의 subtasks로),
    // 태그를 지정하면 그 태그가 붙은 리마인더를 생성순으로, 둘 다 없으면 전체를 생성순으로 조회한다.
    // 태그별/전체 조회에는 하위 작업도 개별 항목으로 포함된다. 모두 현재 사용자가 볼 수 있는 리마인더(본인 것 + 멤버인 리스트의 것)만 조회하며, 멤버가 아닌 리스트는 404다.
    public List<ReminderResponse> getReminders(Long listId, String tag) {
        if (listId != null) {
            listAccess.memberList(listId);
            return toResponses(reminderRepository.findByListIdAndParentIsNull(listId, LIST_SORT));
        }
        if (tag != null) {
            return toResponses(reminderRepository.findAccessibleByTagName(currentUser.id(), tag.trim(), DEFAULT_SORT));
        }
        return toResponses(reminderRepository.findAccessible(currentUser.id(), DEFAULT_SORT));
    }

    public List<ReminderResponse> getSmartReminders(String view) {
        return toResponses(switch (SmartView.from(view)) {
            case TODAY -> {
                LocalDate today = LocalDate.now();
                yield reminderRepository.findAccessibleIncompleteDueBetween(
                        currentUser.id(), today.atStartOfDay(), today.plusDays(1).atStartOfDay(), DUE_DATE_SORT);
            }
            case SCHEDULED -> reminderRepository.findAccessibleIncompleteWithDueAt(currentUser.id(), DUE_DATE_SORT);
            case ALL -> reminderRepository.findAccessibleIncomplete(currentUser.id(), DEFAULT_SORT);
            case FLAGGED -> reminderRepository.findAccessibleIncompleteFlagged(currentUser.id(), DEFAULT_SORT);
            case COMPLETED -> reminderRepository.findAccessibleCompleted(currentUser.id(), COMPLETED_SORT);
        });
    }

    // 알림 스케줄링용. [from, to) 기간에 마감되는 미완료 리마인더를 하위 작업까지 개별 항목으로 마감일시 순으로 조회한다.
    public List<ReminderResponse> getUpcomingReminders(LocalDateTime from, LocalDateTime to) {
        if (!from.isBefore(to)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "from must be before to: from=" + from + ", to=" + to);
        }
        return toResponses(reminderRepository.findAccessibleIncompleteDueBetween(
                currentUser.id(), from, to, DUE_DATE_SORT));
    }

    // 새 리마인더는 같은 리스트(리스트 없음도 하나의 범위)의 마지막 순서로 추가한다.
    // parentId를 지정하면 그 리마인더의 하위 작업으로 추가하며, 이때 listId는 무시하고 부모의 리스트를 따른다.
    @Transactional
    public ReminderResponse createReminder(ReminderRequest request) {
        validateRepeatRule(request.repeatRule(), request.dueAt());
        if (request.parentId() != null) {
            return ReminderResponse.from(createSubtask(request));
        }
        ReminderList list = request.listId() == null ? null : listAccess.memberList(request.listId());
        Reminder reminder = new Reminder(currentUser.reference(),
                request.title(), request.memo(), list, request.dueAt(), request.priority(), request.repeatRule());
        reminder.changeSortOrder(nextSortOrder(list));
        reminder.replaceTags(tagService.resolveTags(request.tagNames()));
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    private Reminder createSubtask(ReminderRequest request) {
        Reminder parent = findReminderOrThrow(request.parentId());
        if (parent.isSubtask()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "A subtask cannot have subtasks: " + request.parentId());
        }
        Reminder subtask = new Reminder(currentUser.reference(),
                request.title(), request.memo(), null, request.dueAt(), request.priority(), request.repeatRule());
        parent.addSubtask(subtask);
        subtask.replaceTags(tagService.resolveTags(request.tagNames()));
        return reminderRepository.save(subtask);
    }

    // 태그도 요청 목록으로 교체한다 (생략하거나 null이면 모두 떨어진다).
    @Transactional
    public ReminderResponse updateReminder(Long id, ReminderUpdateRequest request) {
        Reminder reminder = findReminderOrThrow(id);
        validateRepeatRule(request.repeatRule(), request.dueAt());
        reminder.update(request.title(), request.memo(), request.dueAt(), request.flagged(), request.priority(),
                request.repeatRule());
        reminder.replaceTags(tagService.resolveTags(request.tagNames()));
        return toFlushedResponse(reminder);
    }

    // ids 순서대로 리스트의 최상위 미완료 리마인더 표시 순서를 0부터 다시 매긴다.
    @Transactional
    public void reorderReminders(Long listId, List<Long> ids) {
        listAccess.memberList(listId);
        Map<Long, Reminder> reminders = reminderRepository.findByListIdAndParentIsNullAndCompletedFalse(listId).stream()
                .collect(Collectors.toMap(Reminder::getId, Function.identity()));
        SortOrders.validateIds(ids, reminders.keySet(), "incomplete reminder of the list");
        for (int i = 0; i < ids.size(); i++) {
            reminders.get(ids.get(i)).changeSortOrder(i);
        }
    }

    // 반복 리마인더를 완료하면 다음 회차를 저장한다. 최상위 리마인더의 다음 회차는 같은 리스트의 마지막 순서로 들어가고,
    // 하위 작업의 다음 회차는 도메인에서 같은 부모의 마지막 하위 작업으로 붙는다.
    @Transactional
    public ReminderResponse toggleComplete(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleComplete(LocalDateTime.now()).ifPresent(next -> {
            if (!next.isSubtask()) {
                next.changeSortOrder(nextSortOrder(next.getList()));
            }
            reminderRepository.save(next);
        });
        return toFlushedResponse(reminder);
    }

    @Transactional
    public ReminderResponse toggleFlag(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleFlag();
        return toFlushedResponse(reminder);
    }

    // 하위 작업이 있으면 함께 삭제된다. 하위 작업을 삭제하면 부모의 하위 작업 목록에서도 떼어낸다.
    @Transactional
    public void deleteReminder(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        if (reminder.isSubtask()) {
            reminder.getParent().removeSubtask(reminder);
        }
        reminderRepository.delete(reminder);
    }

    // 리스트의 완료된 리마인더를 모두 삭제한다. 완료된 최상위 리마인더는 하위 작업과 함께 삭제되고(미완료 하위 작업 포함),
    // 미완료 부모 아래의 완료된 하위 작업은 그 하위 작업만 삭제된다. 리스트 멤버면 누구나 할 수 있다.
    @Transactional
    public DeletedCountResponse deleteCompletedReminders(Long listId) {
        listAccess.memberList(listId);
        int deletedCount = 0;
        for (Reminder reminder : reminderRepository.findByListIdAndCompletedTrue(listId)) {
            if (!reminder.isSubtask()) {
                deletedCount += 1 + reminder.getSubtasks().size();
                reminderRepository.delete(reminder);
            } else if (!reminder.getParent().isCompleted()) {
                deletedCount++;
                reminder.getParent().removeSubtask(reminder);
                reminderRepository.delete(reminder);
            }
        }
        return new DeletedCountResponse(deletedCount);
    }

    // 응답 변환은 트랜잭션 안에서 해야 지연 로딩되는 태그/하위 작업/부모를 읽을 수 있다.
    private List<ReminderResponse> toResponses(List<Reminder> reminders) {
        return reminders.stream().map(ReminderResponse::from).toList();
    }

    // 수정일(updatedAt)은 flush할 때 Auditing이 채우므로, 응답에 갱신된 값이 담기도록 먼저 flush한다.
    private ReminderResponse toFlushedResponse(Reminder reminder) {
        reminderRepository.flush();
        return ReminderResponse.from(reminder);
    }

    private void validateRepeatRule(RepeatRule repeatRule, LocalDateTime dueAt) {
        if (repeatRule != null && repeatRule.isRepeating() && dueAt == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "A repeating reminder requires dueAt");
        }
    }

    private int nextSortOrder(ReminderList list) {
        if (list == null) {
            return reminderRepository.findMaxSortOrderWithoutList(currentUser.id()) + 1;
        }
        return reminderRepository.findMaxSortOrderInList(list.getId()) + 1;
    }

    private Reminder findReminderOrThrow(Long id) {
        return reminderRepository.findAccessibleById(id, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Reminder not found: " + id));
    }
}
