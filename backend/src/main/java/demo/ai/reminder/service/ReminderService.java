package demo.ai.reminder.service;

import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.list.ReminderList;
import demo.ai.reminder.domain.reminder.Reminder;
import demo.ai.reminder.dto.request.ReminderOrderRequest;
import demo.ai.reminder.dto.request.ReminderRequest;
import demo.ai.reminder.dto.request.ReminderUpdateRequest;
import demo.ai.reminder.dto.response.DeletedCountResponse;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.dto.response.SliceResponse;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.repository.reminder.ReminderRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ListAccess listAccess;
    private final TagService tagService;
    private final CurrentUser currentUser;
    private final Clock clock;

    public List<ReminderResponse> getReminders(Long listId, String tag) {
        if (listId != null) {
            ReminderList list = listAccess.memberList(listId);
            return toResponses(reminderRepository.findByListAndParentIsNullOrderBySortOrderAscIdAsc(list));
        }
        if (tag != null) {
            return toResponses(reminderRepository.findAccessibleByTagName(currentUser.id(), tag));
        }
        return toResponses(reminderRepository.findAccessible(currentUser.id()));
    }

    public ReminderResponse getReminder(Long id) {
        return ReminderResponse.from(findReminderOrThrow(id));
    }

    public List<ReminderResponse> getSmartReminders(SmartView view, ZoneId zone) {
        return toResponses(switch (view) {
            case TODAY -> findDueToday(zone);
            case SCHEDULED -> reminderRepository.findAccessibleIncompleteWithDueAt(currentUser.id());
            case ALL -> reminderRepository.findAccessibleIncomplete(currentUser.id());
            case FLAGGED -> reminderRepository.findAccessibleIncompleteFlagged(currentUser.id());
        });
    }

    public SliceResponse<ReminderResponse> getCompletedReminders(CompletedCursor cursor, int size) {
        List<Reminder> reminders = cursor == null
                ? reminderRepository.findAccessibleCompleted(currentUser.id(), size + 1)
                : reminderRepository.findAccessibleCompletedBefore(
                        currentUser.id(), cursor.completedAt(), cursor.id(), size + 1);
        return SliceResponse.of(reminders, size, ReminderResponse::from, last -> CompletedCursor.of(last).encode());
    }

    public SliceResponse<ReminderResponse> searchReminders(String query, OffsetCursor cursor, int size) {
        OffsetCursor start = Objects.requireNonNullElse(cursor, OffsetCursor.FIRST);
        String pattern = "%" + escapeLike(query.toLowerCase()) + "%";
        List<Reminder> reminders = reminderRepository.searchAccessible(currentUser.id(), pattern, start.offset(), size + 1);
        return SliceResponse.of(reminders, size, ReminderResponse::from, last -> start.next(size).encode());
    }

    public List<ReminderResponse> getUpcomingReminders(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "from must be before to: from=" + from + ", to=" + to);
        }
        return toResponses(reminderRepository.findAccessibleIncompleteDueBetween(currentUser.id(), from, to));
    }

    @Transactional
    public ReminderResponse createReminder(ReminderRequest request) {
        Reminder parent = request.parentId() == null ? null : findReminderOrThrow(request.parentId());
        ReminderList list = parent == null ? memberListOrNull(request.listId()) : null;
        Reminder reminder = new Reminder(currentUser.reference(),
                request.strippedTitle(), request.memo(), list, request.dueAt(), request.timeZone(), request.priority(),
                request.recurrence());
        if (parent == null) {
            reminder.changeSortOrder(nextSortOrder(list));
        } else {
            parent.addSubtask(reminder);
        }
        reminder.replaceTags(tagService.resolveTags(request.strippedTagNames()));
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Transactional
    public void updateReminder(Long id, ReminderUpdateRequest request) {
        Reminder reminder = findReminderOrThrow(id);
        Versions.verify(reminder.getVersion(), request.version(), "Reminder " + id);
        reminder.update(request.strippedTitle(), request.memo(), request.dueAt(), request.timeZone(), request.flagged(),
                request.priority(), request.recurrence());
        reminder.replaceTags(tagService.resolveTags(request.strippedTagNames()));
    }

    @Transactional
    public void reorderReminders(ReminderOrderRequest request) {
        ReminderList list = listAccess.memberList(request.listId());
        SortOrders.reorder(request.ids(), reminderRepository.findByListAndParentIsNullAndCompletedFalse(list),
                Reminder::getId, Reminder::changeSortOrder, "incomplete reminder of the list");
    }

    @Transactional
    public void moveReminder(Long id, Long listId) {
        Reminder reminder = findReminderOrThrow(id);
        ReminderList target = listAccess.memberList(listId);
        reminder.moveTo(target, () -> nextSortOrder(target));
    }

    @Transactional
    public void toggleComplete(Long id) {
        Reminder reminder = findReminderOrThrow(id);
        reminder.toggleComplete(Instant.now(clock)).ifPresent(next -> {
            if (!next.isSubtask()) {
                next.changeSortOrder(nextSortOrder(next.getList()));
            }
            reminderRepository.save(next);
        });
    }

    @Transactional
    public void toggleFlag(Long id) {
        findReminderOrThrow(id).toggleFlag();
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
        return DeletedCountResponse.from(deletedCount);
    }

    private List<Reminder> findDueToday(ZoneId zone) {
        if (zone == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "zone is required for the today view");
        }
        LocalDate today = LocalDate.now(clock.withZone(zone));
        return reminderRepository.findAccessibleIncompleteDueBetween(currentUser.id(),
                today.atStartOfDay(zone).toInstant(), today.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<ReminderResponse> toResponses(List<Reminder> reminders) {
        return reminders.stream().map(ReminderResponse::from).toList();
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
