package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.repository.ReminderListSummary;

import java.time.LocalDateTime;

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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReminderListResponse from(ReminderListSummary summary) {
        boolean owner = summary.role() == ListRole.OWNER;
        return new ReminderListResponse(
                summary.list().getId(),
                summary.list().getName(),
                summary.list().getColor(),
                summary.list().getIcon(),
                owner && summary.list().getGroup() != null ? summary.list().getGroup().getId() : null,
                summary.list().getSortOrder(),
                summary.reminderCount(),
                summary.role(),
                summary.memberCount(),
                summary.list().getVersion(),
                summary.list().getCreatedAt(),
                summary.list().getUpdatedAt()
        );
    }
}
