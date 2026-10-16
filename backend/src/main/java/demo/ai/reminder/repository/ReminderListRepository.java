package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ReminderList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

// 리스트 접근 권한은 멤버(ListMember) 여부로 판단한다. 소유자도 OWNER 멤버다.
public interface ReminderListRepository extends JpaRepository<ReminderList, Long> {

    // 사용자가 소유한 리스트. 사이드바 순서 변경 대상이다.
    List<ReminderList> findByUserId(Long userId);

    List<ReminderList> findByGroupId(Long groupId);

    // 사용자가 멤버인 리스트의 요약. 리스트 응답의 개수와 역할은 모두 여기서 계산해, 목록과 생성/수정 응답이 같은 규칙을 따른다.
    // 리마인더 개수는 최상위 미완료 리마인더만 센다 (하위 작업 제외).
    String MEMBER_SUMMARY = """
            select new demo.ai.reminder.repository.ReminderListSummary(
                l,
                (select count(r) from Reminder r where r.list = l and r.parent is null and r.completed = false),
                m.role,
                (select count(m2) from ListMember m2 where m2.list = l)
            )
            from ListMember m join m.list l
            where m.user.id = :userId
            """;

    // 사용자가 멤버인 모든 리스트. 사이드바 표시 순서도 여기서 정한다:
    // 소유한 리스트를 사이드바 순서대로 먼저, 공유받은 리스트를 공유받은 순서대로 뒤에 둔다.
    @Query(MEMBER_SUMMARY + """
            order by
                case when m.role = demo.ai.reminder.domain.ListRole.OWNER then 0 else 1 end,
                case when m.role = demo.ai.reminder.domain.ListRole.OWNER then l.sortOrder else 0 end,
                m.id
            """)
    List<ReminderListSummary> findAllWithReminderCount(Long userId);

    // 사용자가 멤버인 리스트 하나의 요약. 멤버가 아니면 비어 있다.
    @Query(MEMBER_SUMMARY + "and l.id = :listId")
    Optional<ReminderListSummary> findSummary(Long userId, Long listId);

    // 새 리스트가 받을 마지막 순서. 리스트가 없으면 첫 순서(0)다.
    @Query("select coalesce(max(l.sortOrder), -1) + 1 from ReminderList l where l.user.id = :userId")
    int findNextSortOrder(Long userId);
}
