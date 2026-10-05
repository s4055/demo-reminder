package demo.ai.reminder.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ListMemberRequest(
        @NotBlank @Email String email
) {
}
