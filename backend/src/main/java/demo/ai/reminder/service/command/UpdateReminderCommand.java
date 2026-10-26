package demo.ai.reminder.service.command;

import demo.ai.reminder.domain.Priority;
import demo.ai.reminder.domain.Recurrence;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateReminderCommand(
        String title,
        String memo,
        LocalDateTime dueAt,
        boolean flagged,
        Priority priority,
        List<String> tagNames,
        Recurrence recurrence,
        Long version
) {
}
