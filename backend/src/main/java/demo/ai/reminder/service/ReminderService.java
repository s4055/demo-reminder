package demo.ai.reminder.service;

import demo.ai.reminder.dto.request.ReminderRequest;
import demo.ai.reminder.dto.request.ReminderUpdateRequest;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.reminder.Reminder;
import demo.ai.reminder.domain.list.ReminderList;
import demo.ai.reminder.dto.response.DeletedCountResponse;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ListAccess listAccess;
    private final TagService tagService;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Transactional
    public ReminderResponse createReminder(ReminderRequest request) {
        Reminder parent = request.parentId() == null ? null : findReminderOrThrow(request.parentId());
        ReminderList list = parent == null ? memberListOrNull(request.listId()) : null;
        Reminder reminder = new Reminder(currentUser.reference(),
                request.strippedTitle(), request.memo(), list, request.dueAt(), request.priority(), request.recurrence());
        if (parent == null) {
            reminder.changeSortOrder(nextSortOrder(list));
        } else {
            parent.addSubtask(reminder);
        }
        reminder.replaceTags(tagService.resolveTags(request.strippedTagNames()));
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Transactional
    public ReminderResponse updateReminder(Long id, ReminderUpdateRequest request) {
        Reminder reminder = findReminderOrThrow(id);
        Versions.verify(reminder.getVersion(), request.version(), "Reminder " + id);
        reminder.update(request.strippedTitle(), request.memo(), request.dueAt(), request.flagged(), request.priority(),
                request.recurrence());
        reminder.replaceTags(tagService.resolveTags(request.strippedTagNames()));
        return toFlushedResponse(reminder);
    }

    @Transactional
    public void reorderReminders(Long listId, List<Long> ids) {
        ReminderList list = listAccess.memberList(listId);
        SortOrders.reorder(ids, reminderRepository.findByListAndParentIsNullAndCompletedFalse(list),
                Reminder::getId, Reminder::changeSortOrder, "incomplete reminder of the list");
    }

    @Transactional
    public ReminderResponse moveReminder(Long id, Long listId) {
        Reminder reminder = findReminderOrThrow(id);
        ReminderList target = listAccess.memberList(listId);
        reminder.moveTo(target, () -> nextSortOrder(target));
        return toFlushedResponse(reminder);
    }

    @Transactional
    public ReminderResponse toggleComplete(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleComplete(LocalDateTime.now(clock)).ifPresent(next -> {
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

    @Transactional
    public void deleteReminder(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.detachFromParent();
        reminderRepository.delete(reminder);
    }

    @Transactional
    public DeletedCountResponse deleteCompletedReminders(Long listId) {
        ReminderList list = listAccess.memberList(listId);
        List<Reminder> targets = reminderRepository.findByListAndCompletedTrue(list).stream()
                .filter(Reminder::isDeletedOnCompletedClear)
                .toList();
        int deletedCount = targets.stream().mapToInt(Reminder::countWithSubtasks).sum();
        targets.forEach(reminder -> {
            reminder.detachFromParent();
            reminderRepository.delete(reminder);
        });
        return new DeletedCountResponse(deletedCount);
    }

    private ReminderResponse toFlushedResponse(Reminder reminder) {
        reminderRepository.flush();
        return ReminderResponse.from(reminder);
    }

    private ReminderList memberListOrNull(Long listId) {
        return listId == null ? null : listAccess.memberList(listId);
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
