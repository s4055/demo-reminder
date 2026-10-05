package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ReminderList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// 리스트 접근 권한은 멤버(ListMember) 여부로 판단한다. 소유자도 OWNER 멤버다.
public interface ReminderListRepository extends JpaRepository<ReminderList, Long> {

    // 사용자가 소유한 리스트. 사이드바 순서 변경 대상이다.
    List<ReminderList> findByUserId(Long userId);

    // 사용자가 멤버인 모든 리스트. 소유한 리스트를 사이드바 순서대로 먼저, 공유받은 리스트를 공유받은 순서대로 뒤에 둔다.
    // 리마인더 개수는 최상위 미완료 리마인더만 센다 (하위 작업 제외).
    @Query("""
            select new demo.ai.reminder.repository.ReminderListSummary(
                l,
                (select count(r) from Reminder r where r.list = l and r.parent is null and r.completed = false),
                m.role,
                (select count(m2) from ListMember m2 where m2.list = l)
            )
            from ListMember m join m.list l
            where m.user.id = :userId
            order by
                case when m.role = demo.ai.reminder.domain.ListRole.OWNER then 0 else 1 end,
                case when m.role = demo.ai.reminder.domain.ListRole.OWNER then l.sortOrder else 0 end,
                m.id
            """)
    List<ReminderListSummary> findAllWithReminderCount(Long userId);

    // 리스트가 없으면 -1을 돌려주므로 +1 하면 첫 순서(0)가 된다.
    @Query("select coalesce(max(l.sortOrder), -1) from ReminderList l where l.user.id = :userId")
    int findMaxSortOrder(Long userId);

    // 공유 기능 이전에 만들어져 멤버(소유자 포함)가 하나도 없는 리스트
    @Query("select l from ReminderList l where not exists (select 1 from ListMember m where m.list = l)")
    List<ReminderList> findWithoutMembers();
}
