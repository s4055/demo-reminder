package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.ReminderList;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 사용자가 볼 수 있는 리마인더는 리스트에 속하면 그 리스트의 멤버인 경우, 리스트가 없으면 본인이 만든 경우다 (ACCESSIBLE).
// 공유받은 리스트의 리마인더는 누가 만들었든 모든 멤버가 볼 수 있다.
// 리스트 기준 조회는 리스트 ID가 아니라 ReminderList를 받는다. 서비스는 접근 권한을 확인한 ListAccess가 돌려준
// 리스트를 넘겨야 하므로, 권한 확인 없이는 호출할 수 없다.
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

    // pattern은 소문자로 바꾸고 LIKE 특수문자(\, %, _)를 \로 이스케이프한 '%검색어%'다.
    // 태그는 조인하면 태그 수만큼 중복되므로 exists로 확인한다.
    @Query("""
            select r from Reminder r
            where (lower(r.title) like :pattern escape '\\'
                   or lower(r.memo) like :pattern escape '\\'
                   or exists (select 1 from r.tags t where lower(t.name) like :pattern escape '\\'))
              and
            """ + ACCESSIBLE)
    List<Reminder> searchAccessible(Long userId, String pattern);

    List<Reminder> findByTagsId(Long tagId);

    List<Reminder> findByListAndParentIsNull(ReminderList list, Sort sort);

    void deleteAllByList(ReminderList list);

    List<Reminder> findByListAndParentIsNullAndCompletedFalse(ReminderList list);

    // 하위 작업도 부모와 같은 리스트에 속하므로 함께 조회된다.
    List<Reminder> findByListAndCompletedTrue(ReminderList list);

    // 아래 두 조회는 새 최상위 리마인더가 받을 마지막 순서이며, 범위가 비어 있으면 첫 순서(0)다.
    // 최상위 리마인더만 대상으로 한다 (하위 작업은 부모 안에서 따로 순서를 매긴다).
    @Query("select coalesce(max(r.sortOrder), -1) + 1 from Reminder r where r.list = :list and r.parent is null")
    int findNextSortOrderInList(ReminderList list);

    // '리스트 없음'은 사용자마다 따로 순서를 매긴다.
    @Query("""
            select coalesce(max(r.sortOrder), -1) + 1 from Reminder r
            where r.user.id = :userId and r.list is null and r.parent is null
            """)
    int findNextSortOrderWithoutList(Long userId);
}
