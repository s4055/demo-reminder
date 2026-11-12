package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.list.ListRole;
import demo.ai.reminder.domain.list.ReminderList;
import demo.ai.reminder.repository.list.ReminderListSummary;

import java.time.Instant;

public record ReminderListResponse(
        Long id,
        String name,
        String color,
        String icon,
        Long groupId,
        int sortOrder,
        long reminderCount,
        ListRole role,
        long memberCount,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReminderListResponse from(ReminderListSummary summary) {
        ReminderList list = summary.list();
        return new ReminderListResponse(
                list.getId(),
                list.getName(),
                list.getColor(),
                list.getIcon(),
                list.groupIdVisibleTo(summary.role()),
                list.getSortOrder(),
                summary.reminderCount(),
                summary.role(),
                summary.memberCount(),
                list.getVersion(),
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }
}
