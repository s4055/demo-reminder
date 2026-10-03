package demo.ai.reminder.repository;

import demo.ai.reminder.domain.Reminder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByListId(Long listId, Sort sort);

    List<Reminder> findByTagsName(String tagName, Sort sort);

    List<Reminder> findByTagsId(Long tagId);

    long countByListIdAndCompletedFalse(Long listId);

    void deleteAllByListId(Long listId);

    List<Reminder> findByCompletedFalse(Sort sort);

    List<Reminder> findByCompletedFalseAndDueAtGreaterThanEqualAndDueAtLessThan(
            LocalDateTime from, LocalDateTime to, Sort sort);

    List<Reminder> findByCompletedFalseAndDueAtIsNotNull(Sort sort);

    List<Reminder> findByCompletedFalseAndFlaggedTrue(Sort sort);

    List<Reminder> findByCompletedTrue(Sort sort);

    List<Reminder> findByListIdAndCompletedFalse(Long listId);

    // 아래 max 조회는 범위가 비어 있으면 -1을 돌려주므로 +1 하면 첫 순서(0)가 된다.
    @Query("select coalesce(max(r.sortOrder), -1) from Reminder r where r.list.id = :listId")
    int findMaxSortOrderInList(Long listId);

    @Query("select coalesce(max(r.sortOrder), -1) from Reminder r where r.list is null")
    int findMaxSortOrderWithoutList();
}
