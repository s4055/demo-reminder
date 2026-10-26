package demo.ai.reminder.repository;

import demo.ai.reminder.domain.ReminderList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReminderListRepository extends JpaRepository<ReminderList, Long> {

    List<ReminderList> findByUserId(Long userId);

    List<ReminderList> findByGroupId(Long groupId);

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

    @Query(MEMBER_SUMMARY + """
            order by
                case when m.role = demo.ai.reminder.domain.ListRole.OWNER then 0 else 1 end,
                case when m.role = demo.ai.reminder.domain.ListRole.OWNER then l.sortOrder else 0 end,
                m.id
            """)
    List<ReminderListSummary> findAllWithReminderCount(Long userId);

    @Query(MEMBER_SUMMARY + "and l.id = :listId")
    Optional<ReminderListSummary> findSummary(Long userId, Long listId);

    @Query("select coalesce(max(l.sortOrder), -1) + 1 from ReminderList l where l.user.id = :userId")
    int findNextSortOrder(Long userId);
}
