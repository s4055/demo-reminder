package demo.ai.demoreminder.dto;

import demo.ai.demoreminder.repository.ReminderListSummary;

import java.time.LocalDateTime;

public record ReminderListResponse(
        Long id,
        String name,
        String color,
        long reminderCount,
        LocalDateTime createdAt
) {

    public static ReminderListResponse from(ReminderListSummary summary) {
        return new ReminderListResponse(
                summary.list().getId(),
                summary.list().getName(),
                summary.list().getColor(),
                summary.reminderCount(),
                summary.list().getCreatedAt()
        );
    }
}
