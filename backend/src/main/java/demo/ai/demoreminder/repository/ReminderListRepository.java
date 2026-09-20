package demo.ai.demoreminder.repository;

import demo.ai.demoreminder.domain.ReminderList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReminderListRepository extends JpaRepository<ReminderList, Long> {

    @Query("""
            select new demo.ai.demoreminder.repository.ReminderListSummary(
                l,
                (select count(r) from Reminder r where r.list = l and r.completed = false)
            )
            from ReminderList l
            order by l.id
            """)
    List<ReminderListSummary> findAllWithReminderCount();
}
