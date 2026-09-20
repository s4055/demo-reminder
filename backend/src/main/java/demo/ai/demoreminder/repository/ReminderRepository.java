package demo.ai.demoreminder.repository;

import demo.ai.demoreminder.domain.Reminder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByListId(Long listId, Sort sort);

    long countByListIdAndCompletedFalse(Long listId);

    void deleteAllByListId(Long listId);
}
