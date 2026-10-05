package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Reminder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 사용자가 볼 수 있는 리마인더는 리스트에 속하면 그 리스트의 멤버인 경우, 리스트가 없으면 본인이 만든 경우다 (ACCESSIBLE).
// 공유받은 리스트의 리마인더는 누가 만들었든 모든 멤버가 볼 수 있다.
// 리스트(listId) 기준 조회는 서비스에서 리스트 멤버인지 먼저 확인한 뒤 호출한다.
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    String ACCESSIBLE = """
            ((r.list is null and r.user.id = :userId)
             or exists (select 1 from ListMember m where m.list = r.list and m.user.id = :userId))
            """;

    @Query("select r from Reminder r where r.id = :id and " + ACCESSIBLE)
    Optional<Reminder> findAccessibleById(Long id, Long userId);

    @Query("select r from Reminder r where " + ACCESSIBLE)
    List<Reminder> findAccessible(Long userId, Sort sort);

    // 같은 이름의 태그라도 사용자마다 따로 있으므로 이름으로 찾는다 (공유 리스트에서 다른 멤버가 붙인 태그 포함).
    @Query("select r from Reminder r join r.tags t where t.name = :tagName and " + ACCESSIBLE)
    List<Reminder> findAccessibleByTagName(Long userId, String tagName, Sort sort);

    @Query("select r from Reminder r where r.completed = false and " + ACCESSIBLE)
    List<Reminder> findAccessibleIncomplete(Long userId, Sort sort);

    @Query("""
            select r from Reminder r
            where r.completed = false and r.dueAt >= :from and r.dueAt < :to and
            """ + ACCESSIBLE)
    List<Reminder> findAccessibleIncompleteDueBetween(Long userId, LocalDateTime from, LocalDateTime to, Sort sort);

    @Query("select r from Reminder r where r.completed = false and r.dueAt is not null and " + ACCESSIBLE)
    List<Reminder> findAccessibleIncompleteWithDueAt(Long userId, Sort sort);

    @Query("select r from Reminder r where r.completed = false and r.flagged = true and " + ACCESSIBLE)
    List<Reminder> findAccessibleIncompleteFlagged(Long userId, Sort sort);

    @Query("select r from Reminder r where r.completed = true and " + ACCESSIBLE)
    List<Reminder> findAccessibleCompleted(Long userId, Sort sort);

    List<Reminder> findByListIdAndParentIsNull(Long listId, Sort sort);

    List<Reminder> findByTagsId(Long tagId);

    long countByListIdAndParentIsNullAndCompletedFalse(Long listId);

    void deleteAllByListId(Long listId);

    List<Reminder> findByListIdAndParentIsNullAndCompletedFalse(Long listId);

    // 아래 max 조회는 최상위 리마인더만 대상으로 하며(하위 작업은 부모 안에서 따로 순서를 매긴다),
    // 범위가 비어 있으면 -1을 돌려주므로 +1 하면 첫 순서(0)가 된다.
    @Query("select coalesce(max(r.sortOrder), -1) from Reminder r where r.list.id = :listId and r.parent is null")
    int findMaxSortOrderInList(Long listId);

    // '리스트 없음'은 사용자마다 따로 순서를 매긴다.
    @Query("""
            select coalesce(max(r.sortOrder), -1) from Reminder r
            where r.user.id = :userId and r.list is null and r.parent is null
            """)
    int findMaxSortOrderWithoutList(Long userId);
}
