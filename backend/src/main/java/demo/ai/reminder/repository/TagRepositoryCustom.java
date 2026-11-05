package demo.ai.reminder.repository;

import java.util.List;

public interface TagRepositoryCustom {

    List<TagSummary> findAllInUseWithReminderCount(Long userId);
}
