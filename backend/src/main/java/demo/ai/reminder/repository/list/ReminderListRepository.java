package demo.ai.reminder.repository.list;

import demo.ai.reminder.domain.list.ReminderList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReminderListRepository extends JpaRepository<ReminderList, Long>, ReminderListRepositoryCustom {

    List<ReminderList> findByUserId(Long userId);

    List<ReminderList> findByGroupId(Long groupId);

    @Query("select coalesce(max(l.sortOrder), -1) + 1 from ReminderList l where l.user.id = :userId")
    int findNextSortOrder(Long userId);
}
