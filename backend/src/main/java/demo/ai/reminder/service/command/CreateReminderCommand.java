package demo.ai.reminder.service.command;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;

import java.time.LocalDateTime;
import java.util.List;

public record CreateReminderCommand(
        String title,
        String memo,
        Long listId,
        LocalDateTime dueAt,
        Priority priority,
        List<String> tagNames,
        Long parentId,
        Recurrence recurrence
) {
}
