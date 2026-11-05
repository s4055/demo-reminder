package demo.ai.reminder.repository;

import demo.ai.reminder.domain.reminder.Reminder;
import demo.ai.reminder.domain.list.ReminderList;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long>, ReminderRepositoryCustom {

    List<Reminder> findByListAndParentIsNull(ReminderList list, Sort sort);

    void deleteAllByList(ReminderList list);

    List<Reminder> findByListAndParentIsNullAndCompletedFalse(ReminderList list);

    List<Reminder> findByListAndCompletedTrue(ReminderList list);

    @Query("select coalesce(max(r.sortOrder), -1) + 1 from Reminder r where r.list = :list and r.parent is null")
    int findNextSortOrderInList(ReminderList list);

    @Query("select coalesce(max(r.sortOrder), -1) + 1 from Reminder r where r.user.id = :userId and r.list is null and r.parent is null")
    int findNextSortOrderWithoutList(Long userId);
}
