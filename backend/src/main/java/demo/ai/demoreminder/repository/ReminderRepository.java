package demo.ai.demoreminder.repository;

import demo.ai.demoreminder.domain.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
}
