package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import demo.ai.reminder.dto.response.ReminderResponse;
import demo.ai.reminder.repository.ReminderRepository;
import demo.ai.reminder.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReminderQueryService {

    private static final Sort DEFAULT_SORT = Sort.by("id");
    private static final Sort LIST_SORT = Sort.by("sortOrder", "id");
    private static final Sort DUE_DATE_SORT = Sort.by("dueAt", "id");
    private static final Sort COMPLETED_SORT = Sort.by(Sort.Order.desc("completedAt"), Sort.Order.desc("id"));
    private static final Comparator<Reminder> SEARCH_ORDER = Comparator
            .comparing(Reminder::isCompleted)
            .thenComparing(reminder -> reminder.isCompleted() ? null : reminder.getDueAt(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(reminder -> reminder.isCompleted() ? reminder.getCompletedAt() : null,
                    Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()))
            .thenComparing(Reminder::getId);

    private final ReminderRepository reminderRepository;
    private final ListAccess listAccess;
    private final CurrentUser currentUser;
    private final Clock clock;

    public List<ReminderResponse> getReminders(Long listId, String tag) {
        if (listId != null) {
            ReminderList list = listAccess.memberList(listId);
            return toResponses(reminderRepository.findByListAndParentIsNull(list, LIST_SORT));
        }
        if (tag != null) {
            return toResponses(reminderRepository.findAccessibleByTagName(currentUser.id(), tag, DEFAULT_SORT));
        }
        return toResponses(reminderRepository.findAccessible(currentUser.id(), DEFAULT_SORT));
    }

    public List<ReminderResponse> getSmartReminders(SmartView view) {
        return toResponses(switch (view) {
            case TODAY -> {
                LocalDate today = LocalDate.now(clock);
                yield reminderRepository.findAccessibleIncompleteDueBetween(
                        currentUser.id(), today.atStartOfDay(), today.plusDays(1).atStartOfDay(), DUE_DATE_SORT);
            }
            case SCHEDULED -> reminderRepository.findAccessibleIncompleteWithDueAt(currentUser.id(), DUE_DATE_SORT);
            case ALL -> reminderRepository.findAccessibleIncomplete(currentUser.id(), DEFAULT_SORT);
            case FLAGGED -> reminderRepository.findAccessibleIncompleteFlagged(currentUser.id(), DEFAULT_SORT);
            case COMPLETED -> reminderRepository.findAccessibleCompleted(currentUser.id(), COMPLETED_SORT);
        });
    }

    public List<ReminderResponse> searchReminders(String query) {
        return reminderRepository.searchAccessible(currentUser.id(), "%" + escapeLike(query.toLowerCase()) + "%")
                .stream()
                .sorted(SEARCH_ORDER)
                .map(ReminderResponse::from)
                .toList();
    }

    public List<ReminderResponse> getUpcomingReminders(LocalDateTime from, LocalDateTime to) {
        if (!from.isBefore(to)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "from must be before to: from=" + from + ", to=" + to);
        }
        return toResponses(reminderRepository.findAccessibleIncompleteDueBetween(
                currentUser.id(), from, to, DUE_DATE_SORT));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<ReminderResponse> toResponses(List<Reminder> reminders) {
        return reminders.stream().map(ReminderResponse::from).toList();
    }
}
