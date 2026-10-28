package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.ListRole;
import demo.ai.reminder.domain.ReminderList;
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
        ReminderList list = summary.list();
        return new ReminderListResponse(
                list.getId(),
                list.getName(),
                list.getColor(),
                list.getIcon(),
                groupIdVisibleTo(summary.role(), list),
                list.getSortOrder(),
                summary.reminderCount(),
                summary.role(),
                summary.memberCount(),
                list.getVersion(),
                list.getCreatedAt(),
                list.getUpdatedAt()
        );
    }

    private static Long groupIdVisibleTo(ListRole role, ReminderList list) {
        if (role != ListRole.OWNER || list.getGroup() == null) {
            return null;
        }
        return list.getGroup().getId();
    }
}
