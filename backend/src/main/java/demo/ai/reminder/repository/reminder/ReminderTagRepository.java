package demo.ai.reminder.repository.reminder;

import demo.ai.reminder.domain.reminder.ReminderTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderTagRepository extends JpaRepository<ReminderTag, Long> {

    void deleteByTag_Id(Long tagId);
}
