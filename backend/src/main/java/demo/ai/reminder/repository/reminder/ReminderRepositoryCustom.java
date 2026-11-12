package demo.ai.reminder.repository.reminder;

import demo.ai.reminder.domain.reminder.Reminder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReminderRepositoryCustom {

    Optional<Reminder> findAccessibleById(Long id, Long userId);

    List<Reminder> findAccessible(Long userId);

    List<Reminder> findAccessibleByTagName(Long userId, String tagName);

    List<Reminder> findAccessibleIncomplete(Long userId);

    List<Reminder> findAccessibleIncompleteDueBetween(Long userId, Instant from, Instant to);

    List<Reminder> findAccessibleIncompleteWithDueAt(Long userId);

    List<Reminder> findAccessibleIncompleteFlagged(Long userId);

    List<Reminder> findAccessibleCompleted(Long userId, int limit);

    List<Reminder> findAccessibleCompletedBefore(Long userId, Instant completedAt, Long id, int limit);

    List<Reminder> searchAccessible(Long userId, String pattern, long offset, int limit);
}
