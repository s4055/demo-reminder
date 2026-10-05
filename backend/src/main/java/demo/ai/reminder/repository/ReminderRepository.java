package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Reminder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 사용자 데이터 격리를 위해 목록/단건 조회는 모두 소유자(userId) 조건을 붙인다.
// 리스트(listId) 기준 조회는 서비스에서 리스트 소유자를 먼저 확인한 뒤 호출한다.
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);

    List<Reminder> findByUserId(Long userId, Sort sort);

    List<Reminder> findByListIdAndParentIsNull(Long listId, Sort sort);

    List<Reminder> findByUserIdAndTagsName(Long userId, String tagName, Sort sort);

    List<Reminder> findByTagsId(Long tagId);

    long countByListIdAndParentIsNullAndCompletedFalse(Long listId);

    void deleteAllByListId(Long listId);

    List<Reminder> findByUserIdAndCompletedFalse(Long userId, Sort sort);

    List<Reminder> findByUserIdAndCompletedFalseAndDueAtGreaterThanEqualAndDueAtLessThan(
            Long userId, LocalDateTime from, LocalDateTime to, Sort sort);

    List<Reminder> findByUserIdAndCompletedFalseAndDueAtIsNotNull(Long userId, Sort sort);

    List<Reminder> findByUserIdAndCompletedFalseAndFlaggedTrue(Long userId, Sort sort);

    List<Reminder> findByUserIdAndCompletedTrue(Long userId, Sort sort);

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
