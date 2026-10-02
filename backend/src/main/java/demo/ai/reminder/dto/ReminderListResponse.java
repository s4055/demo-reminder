package demo.ai.reminder.dto;

import demo.ai.reminder.repository.ReminderListSummary;

import java.time.LocalDateTime;

public record ReminderListResponse(
        Long id,
        String name,
        String color,
        int sortOrder,
        long reminderCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReminderListResponse from(ReminderListSummary summary) {
        return new ReminderListResponse(
                summary.list().getId(),
                summary.list().getName(),
                summary.list().getColor(),
                summary.list().getSortOrder(),
                summary.reminderCount(),
                summary.list().getCreatedAt(),
                summary.list().getUpdatedAt()
        );
    }
}
