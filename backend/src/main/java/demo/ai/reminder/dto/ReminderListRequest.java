package demo.ai.reminder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReminderListRequest(
        @NotBlank String name,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "color must be a hex color like #FF9500") String color
) {
}
