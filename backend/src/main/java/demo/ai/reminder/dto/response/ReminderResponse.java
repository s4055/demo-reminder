package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.RepeatRule;
import demo.ai.reminder.domain.Tag;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public record ReminderResponse(
        Long id,
        String title,
        String memo,
        boolean completed,
        boolean flagged,
        Priority priority,
        LocalDateTime dueAt,
        RepeatRule repeatRule,
        int repeatInterval,
        List<DayOfWeek> repeatDaysOfWeek,
        LocalDateTime completedAt,
        Long listId,
        int sortOrder,
        List<String> tags,
        Long parentId,
        List<ReminderResponse> subtasks,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReminderResponse from(Reminder reminder) {
        Recurrence recurrence = reminder.getRecurrence();
        return new ReminderResponse(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getMemo(),
                reminder.isCompleted(),
                reminder.isFlagged(),
                reminder.getPriority(),
                reminder.getDueAt(),
                recurrence.rule(),
                recurrence.interval(),
                recurrence.daysOfWeek().stream().sorted().toList(),
                reminder.getCompletedAt(),
                reminder.getList() != null ? reminder.getList().getId() : null,
                reminder.getSortOrder(),
                reminder.getTags().stream().map(Tag::getName).sorted().toList(),
                reminder.getParent() != null ? reminder.getParent().getId() : null,
                reminder.getSubtasks().stream().map(ReminderResponse::from).toList(),
                reminder.getVersion(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
