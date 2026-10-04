package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.domain.RepeatRule;
import demo.ai.reminder.dto.ReminderRequest;
import demo.ai.reminder.dto.ReminderUpdateRequest;
import demo.ai.reminder.repository.ReminderListRepository;
import demo.ai.reminder.repository.ReminderRepository;
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
    private final ReminderListRepository reminderListRepository;
    private final TagService tagService;

    // 리스트를 지정하면 최상위 리마인더만 표시 순서대로(하위 작업은 각 부모의 subtasks로),
    // 태그를 지정하면 그 태그가 붙은 리마인더를 생성순으로, 둘 다 없으면 전체를 생성순으로 조회한다.
    // 태그별/전체 조회에는 하위 작업도 개별 항목으로 포함된다.
    public List<Reminder> getReminders(Long listId, String tag) {
        if (listId != null) {
            return reminderRepository.findByListIdAndParentIsNull(listId, LIST_SORT);
        }
        if (tag != null) {
            return reminderRepository.findByTagsName(tag.trim(), DEFAULT_SORT);
        }
        return reminderRepository.findAll(DEFAULT_SORT);
    }

    public List<Reminder> getSmartReminders(String view) {
        return switch (SmartView.from(view)) {
            case TODAY -> {
                LocalDate today = LocalDate.now();
                yield reminderRepository.findByCompletedFalseAndDueAtGreaterThanEqualAndDueAtLessThan(
                        today.atStartOfDay(), today.plusDays(1).atStartOfDay(), DUE_DATE_SORT);
            }
            case SCHEDULED -> reminderRepository.findByCompletedFalseAndDueAtIsNotNull(DUE_DATE_SORT);
            case ALL -> reminderRepository.findByCompletedFalse(DEFAULT_SORT);
            case FLAGGED -> reminderRepository.findByCompletedFalseAndFlaggedTrue(DEFAULT_SORT);
            case COMPLETED -> reminderRepository.findByCompletedTrue(COMPLETED_SORT);
        };
    }

    // 알림 스케줄링용. [from, to) 기간에 마감되는 미완료 리마인더를 하위 작업까지 개별 항목으로 마감일시 순으로 조회한다.
    public List<Reminder> getUpcomingReminders(LocalDateTime from, LocalDateTime to) {
        if (!from.isBefore(to)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "from must be before to: from=" + from + ", to=" + to);
        }
        return reminderRepository.findByCompletedFalseAndDueAtGreaterThanEqualAndDueAtLessThan(from, to, DUE_DATE_SORT);
    }

    // 새 리마인더는 같은 리스트(리스트 없음도 하나의 범위)의 마지막 순서로 추가한다.
    // parentId를 지정하면 그 리마인더의 하위 작업으로 추가하며, 이때 listId는 무시하고 부모의 리스트를 따른다.
    @Transactional
    public Reminder createReminder(ReminderRequest request) {
        validateRepeatRule(request.repeatRule(), request.dueAt());
        if (request.parentId() != null) {
            return createSubtask(request);
        }
        ReminderList list = request.listId() == null ? null : findListOrThrow(request.listId());
        Reminder reminder = new Reminder(
                request.title(), request.memo(), list, request.dueAt(), request.priority(), request.repeatRule());
        reminder.changeSortOrder(nextSortOrder(list));
        reminder.replaceTags(tagService.resolveTags(request.tagNames()));
        return reminderRepository.save(reminder);
    }

    private Reminder createSubtask(ReminderRequest request) {
        Reminder parent = findReminderOrThrow(request.parentId());
        if (parent.isSubtask()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "A subtask cannot have subtasks: " + request.parentId());
        }
        Reminder subtask = new Reminder(
                request.title(), request.memo(), null, request.dueAt(), request.priority(), request.repeatRule());
        parent.addSubtask(subtask);
        subtask.replaceTags(tagService.resolveTags(request.tagNames()));
        return reminderRepository.save(subtask);
    }

    // 태그도 요청 목록으로 교체한다 (생략하거나 null이면 모두 떨어진다).
    @Transactional
    public Reminder updateReminder(Long id, ReminderUpdateRequest request) {
        Reminder reminder = findReminderOrThrow(id);
        validateRepeatRule(request.repeatRule(), request.dueAt());
        reminder.update(request.title(), request.memo(), request.dueAt(), request.flagged(), request.priority(),
                request.repeatRule());
        reminder.replaceTags(tagService.resolveTags(request.tagNames()));
        return reminder;
    }

    // ids 순서대로 리스트의 최상위 미완료 리마인더 표시 순서를 0부터 다시 매긴다.
    @Transactional
    public void reorderReminders(Long listId, List<Long> ids) {
        findListOrThrow(listId);
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
    public Reminder toggleComplete(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleComplete(LocalDateTime.now()).ifPresent(next -> {
            if (!next.isSubtask()) {
                next.changeSortOrder(nextSortOrder(next.getList()));
            }
            reminderRepository.save(next);
        });
        return reminder;
    }

    @Transactional
    public Reminder toggleFlag(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleFlag();
        return reminder;
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

    private void validateRepeatRule(RepeatRule repeatRule, LocalDateTime dueAt) {
        if (repeatRule != null && repeatRule.isRepeating() && dueAt == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "A repeating reminder requires dueAt");
        }
    }

    private int nextSortOrder(ReminderList list) {
        if (list == null) {
            return reminderRepository.findMaxSortOrderWithoutList() + 1;
        }
        return reminderRepository.findMaxSortOrderInList(list.getId()) + 1;
    }

    private ReminderList findListOrThrow(Long listId) {
        return reminderListRepository.findById(listId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "List not found: " + listId));
    }

    private Reminder findReminderOrThrow(Long id) {
        return reminderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Reminder not found: " + id));
    }
}
