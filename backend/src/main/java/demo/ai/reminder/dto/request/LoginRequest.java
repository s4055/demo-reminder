package demo.ai.reminder.dto.request;

import demo.ai.reminder.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = User.EMAIL_MAX_LENGTH) String email,
        @NotBlank String password
) {
}
