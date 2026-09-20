package demo.ai.demoreminder.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record ReminderUpdateRequest(
        @NotBlank String title,
        String memo,
        LocalDateTime dueAt,
        boolean flagged
) {
}
