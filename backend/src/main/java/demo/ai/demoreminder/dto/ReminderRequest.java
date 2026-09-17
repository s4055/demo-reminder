package demo.ai.demoreminder.dto;

import jakarta.validation.constraints.NotBlank;

public record ReminderRequest(
        @NotBlank String title,
        String memo
) {
}
