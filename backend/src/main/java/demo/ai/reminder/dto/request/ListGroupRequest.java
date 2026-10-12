package demo.ai.reminder.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ListGroupRequest(
        @NotBlank String name
) {
}
