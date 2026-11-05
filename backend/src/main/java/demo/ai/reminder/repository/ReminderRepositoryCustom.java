package demo.ai.reminder.repository;

import demo.ai.reminder.domain.reminder.Reminder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReminderRepositoryCustom {

    Optional<Reminder> findAccessibleById(Long id, Long userId);

    List<Reminder> findAccessible(Long userId);

    List<Reminder> findAccessibleByTagName(Long userId, String tagName);

    List<Reminder> findAccessibleIncomplete(Long userId);

    List<Reminder> findAccessibleIncompleteDueBetween(Long userId, LocalDateTime from, LocalDateTime to);

    List<Reminder> findAccessibleIncompleteWithDueAt(Long userId);

    List<Reminder> findAccessibleIncompleteFlagged(Long userId);

    List<Reminder> findAccessibleCompleted(Long userId);

    List<Reminder> searchAccessible(Long userId, String pattern);
}
