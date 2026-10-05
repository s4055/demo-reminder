package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ReminderList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// 사용자 데이터 격리를 위해 모든 조회에 소유자(userId) 조건을 붙인다.
public interface ReminderListRepository extends JpaRepository<ReminderList, Long> {

    Optional<ReminderList> findByIdAndUserId(Long id, Long userId);

    List<ReminderList> findByUserId(Long userId);

    // 리마인더 개수는 최상위 미완료 리마인더만 센다 (하위 작업 제외).
    @Query("""
            select new demo.ai.reminder.repository.ReminderListSummary(
                l,
                (select count(r) from Reminder r where r.list = l and r.parent is null and r.completed = false)
            )
            from ReminderList l
            where l.user.id = :userId
            order by l.sortOrder, l.id
            """)
    List<ReminderListSummary> findAllWithReminderCount(Long userId);

    // 리스트가 없으면 -1을 돌려주므로 +1 하면 첫 순서(0)가 된다.
    @Query("select coalesce(max(l.sortOrder), -1) from ReminderList l where l.user.id = :userId")
    int findMaxSortOrder(Long userId);
}
