package demo.ai.reminder.repository.tag;

import java.util.List;

public interface TagRepositoryCustom {

    List<TagSummary> findAllInUseWithReminderCount(Long userId);
}
