package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ReminderList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReminderListRepository extends JpaRepository<ReminderList, Long> {

    // 리마인더 개수는 최상위 미완료 리마인더만 센다 (하위 작업 제외).
    @Query("""
            select new demo.ai.reminder.repository.ReminderListSummary(
                l,
                (select count(r) from Reminder r where r.list = l and r.parent is null and r.completed = false)
            )
            from ReminderList l
            order by l.sortOrder, l.id
            """)
    List<ReminderListSummary> findAllWithReminderCount();

    // 리스트가 없으면 -1을 돌려주므로 +1 하면 첫 순서(0)가 된다.
    @Query("select coalesce(max(l.sortOrder), -1) from ReminderList l")
    int findMaxSortOrder();
}
