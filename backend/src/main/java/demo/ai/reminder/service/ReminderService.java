package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.response.DeletedCountResponse;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.security.CurrentUser;
import demo.ai.reminder.service.command.CreateReminderCommand;
import demo.ai.reminder.service.command.UpdateReminderCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 리마인더 생성/수정/이동/완료/삭제. 조회는 ReminderQueryService가 맡는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ListAccess listAccess;
    private final TagService tagService;
    private final CurrentUser currentUser;

    // 새 리마인더는 같은 리스트(리스트 없음도 하나의 범위)의 마지막 순서로 추가한다.
    // parentId를 지정하면 그 리마인더의 하위 작업으로 추가하며, 이때 listId는 무시하고 부모의 리스트를 따른다.
    // 마감일시 없이 반복하면 도메인이 DomainException(400)을 던진다.
    @Transactional
    public ReminderResponse createReminder(CreateReminderCommand command) {
        if (command.parentId() != null) {
            return ReminderResponse.from(createSubtask(command));
        }
        ReminderList list = command.listId() == null ? null : listAccess.memberList(command.listId());
        Reminder reminder = new Reminder(currentUser.reference(),
                command.title(), command.memo(), list, command.dueAt(), command.priority(), command.recurrence());
        reminder.changeSortOrder(nextSortOrder(list));
        reminder.replaceTags(tagService.resolveTags(command.tagNames()));
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    private Reminder createSubtask(CreateReminderCommand command) {
        Reminder parent = findReminderOrThrow(command.parentId());
        Reminder subtask = new Reminder(currentUser.reference(),
                command.title(), command.memo(), null, command.dueAt(), command.priority(), command.recurrence());
        parent.addSubtask(subtask);
        subtask.replaceTags(tagService.resolveTags(command.tagNames()));
        return reminderRepository.save(subtask);
    }

    // 태그도 요청 목록으로 교체한다 (생략하거나 null이면 모두 떨어진다).
    // 요청 버전이 현재 버전과 다르면(편집 중 다른 사용자가 먼저 수정) 409다.
    @Transactional
    public ReminderResponse updateReminder(Long id, UpdateReminderCommand command) {
        Reminder reminder = findReminderOrThrow(id);
        Versions.verify(reminder.getVersion(), command.version(), "Reminder " + id);
        reminder.update(command.title(), command.memo(), command.dueAt(), command.flagged(), command.priority(),
                command.recurrence());
        reminder.replaceTags(tagService.resolveTags(command.tagNames()));
        return toFlushedResponse(reminder);
    }

    // ids 순서대로 리스트의 최상위 미완료 리마인더 표시 순서를 0부터 다시 매긴다.
    @Transactional
    public void reorderReminders(Long listId, List<Long> ids) {
        ReminderList list = listAccess.memberList(listId);
        SortOrders.reorder(ids, reminderRepository.findByListAndParentIsNullAndCompletedFalse(list),
                Reminder::getId, Reminder::changeSortOrder, "incomplete reminder of the list");
    }

    // 최상위 리마인더를 하위 작업과 함께 다른 리스트의 마지막 순서로 옮긴다. 대상 리스트의 멤버여야 하고(아니면 404),
    // 하위 작업은 단독으로 옮길 수 없다(400). 이미 그 리스트에 있으면 순서도 바꾸지 않는다.
    @Transactional
    public ReminderResponse moveReminder(Long id, Long listId) {
        Reminder reminder = findReminderOrThrow(id);
        // 같은 리스트로 옮기는 요청은 아래에서 도메인을 거치지 않고 끝나므로, 하위 작업 검사는 여기서 먼저 한다.
        if (reminder.isSubtask()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "A subtask cannot be moved on its own: " + id);
        }
        ReminderList target = listAccess.memberList(listId);
        if (reminder.getList() != null && reminder.getList().getId().equals(target.getId())) {
            return ReminderResponse.from(reminder);
        }
        reminder.moveTo(target, nextSortOrder(target));
        return toFlushedResponse(reminder);
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
        reminder.detachFromParent();
        reminderRepository.delete(reminder);
    }

    // 리스트의 완료된 리마인더를 모두 삭제한다. 완료된 최상위 리마인더는 하위 작업과 함께 삭제되고(미완료 하위 작업 포함),
    // 미완료 부모 아래의 완료된 하위 작업은 그 하위 작업만 삭제된다. 리스트 멤버면 누구나 할 수 있다.
    @Transactional
    public DeletedCountResponse deleteCompletedReminders(Long listId) {
        ReminderList list = listAccess.memberList(listId);
        int deletedCount = 0;
        for (Reminder reminder : reminderRepository.findByListAndCompletedTrue(list)) {
            if (!reminder.isSubtask()) {
                deletedCount += 1 + reminder.getSubtasks().size();
                reminderRepository.delete(reminder);
            } else if (!reminder.getParent().isCompleted()) {
                deletedCount++;
                reminder.detachFromParent();
                reminderRepository.delete(reminder);
            }
        }
        return new DeletedCountResponse(deletedCount);
    }

    // 수정일(updatedAt)은 flush할 때 Auditing이 채우므로, 응답에 갱신된 값이 담기도록 먼저 flush한다.
    private ReminderResponse toFlushedResponse(Reminder reminder) {
        reminderRepository.flush();
        return ReminderResponse.from(reminder);
    }

    private int nextSortOrder(ReminderList list) {
        if (list == null) {
            return reminderRepository.findNextSortOrderWithoutList(currentUser.id());
        }
        return reminderRepository.findNextSortOrderInList(list);
    }

    private Reminder findReminderOrThrow(Long id) {
        return reminderRepository.findAccessibleById(id, currentUser.id())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "Reminder not found: " + id));
    }
}
