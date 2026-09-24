package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Reminder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByListId(Long listId, Sort sort);

    long countByListIdAndCompletedFalse(Long listId);

    void deleteAllByListId(Long listId);

    List<Reminder> findByCompletedFalse(Sort sort);

    List<Reminder> findByCompletedFalseAndDueAtGreaterThanEqualAndDueAtLessThan(
            LocalDateTime from, LocalDateTime to, Sort sort);

    List<Reminder> findByCompletedFalseAndDueAtIsNotNull(Sort sort);

    List<Reminder> findByCompletedFalseAndFlaggedTrue(Sort sort);

    List<Reminder> findByCompletedTrue(Sort sort);
}
