package demo.ai.reminder.dto;

import demo.ai.reminder.domain.Priority;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record ReminderRequest(
        @NotBlank String title,
        String memo,
        Long listId,
        LocalDateTime dueAt,
        Priority priority
) {
}
