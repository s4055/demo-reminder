package demo.ai.reminder.dto;

import jakarta.validation.constraints.NotBlank;

public record ListGroupRequest(
        @NotBlank String name
) {
}
