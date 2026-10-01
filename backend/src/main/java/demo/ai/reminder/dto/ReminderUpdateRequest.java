package demo.ai.reminder.dto;

import demo.ai.reminder.domain.Priority;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record ReminderUpdateRequest(
        @NotBlank String title,
        String memo,
        LocalDateTime dueAt,
        boolean flagged,
        Priority priority
) {
}
