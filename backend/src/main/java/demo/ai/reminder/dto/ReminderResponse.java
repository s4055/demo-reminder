package demo.ai.reminder.dto;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Reminder;

import java.time.LocalDateTime;

public record ReminderResponse(
        Long id,
        String title,
        String memo,
        boolean completed,
        boolean flagged,
        Priority priority,
        LocalDateTime dueAt,
        LocalDateTime completedAt,
        Long listId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getMemo(),
                reminder.isCompleted(),
                reminder.isFlagged(),
                reminder.getPriority(),
                reminder.getDueAt(),
                reminder.getCompletedAt(),
                reminder.getList() != null ? reminder.getList().getId() : null,
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
