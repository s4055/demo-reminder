package demo.ai.reminder.dto.request;

import demo.ai.reminder.domain.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ListMemberRequest(
        @NotBlank @Email @Size(max = User.EMAIL_MAX_LENGTH) String email
) {

    public String normalizedEmail() {
        return User.normalizeEmail(email);
    }
}
