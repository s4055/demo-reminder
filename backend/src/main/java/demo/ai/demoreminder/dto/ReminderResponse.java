package demo.ai.demoreminder.dto;

import demo.ai.demoreminder.domain.Reminder;

import java.time.LocalDateTime;

public record ReminderResponse(
        Long id,
        String title,
        String memo,
        boolean completed,
        boolean flagged,
        LocalDateTime dueAt,
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
                reminder.getDueAt(),
                reminder.getList() != null ? reminder.getList().getId() : null,
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
