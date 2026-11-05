package demo.ai.reminder.repository.list;

import java.util.List;
import java.util.Optional;

public interface ReminderListRepositoryCustom {

    List<ReminderListSummary> findAllWithReminderCount(Long userId);

    Optional<ReminderListSummary> findSummary(Long userId, Long listId);
}
