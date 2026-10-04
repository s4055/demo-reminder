package demo.ai.reminder.dto;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Reminder;
import demo.ai.reminder.domain.RepeatRule;
import demo.ai.reminder.domain.Tag;

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
        LocalDateTime completedAt,
        Long listId,
        int sortOrder,
        List<String> tags,
        Long parentId,
        List<ReminderResponse> subtasks,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // 하위 작업은 1단계까지만 있으므로 subtasks의 각 항목은 빈 subtasks를 가진다.
    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getMemo(),
                reminder.isCompleted(),
                reminder.isFlagged(),
                reminder.getPriority(),
                reminder.getDueAt(),
                reminder.getRepeatRule(),
                reminder.getCompletedAt(),
                reminder.getList() != null ? reminder.getList().getId() : null,
                reminder.getSortOrder(),
                reminder.getTags().stream().map(Tag::getName).sorted().toList(),
                reminder.getParent() != null ? reminder.getParent().getId() : null,
                reminder.getSubtasks().stream().map(ReminderResponse::from).toList(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
