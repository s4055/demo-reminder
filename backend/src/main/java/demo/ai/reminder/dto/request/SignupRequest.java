package demo.ai.reminder.dto.request;

import demo.ai.reminder.dto.validation.MaxBytes;
import demo.ai.reminder.service.command.SignupCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email @Size(max = EMAIL_MAX_LENGTH) String email,
        @NotBlank @Size(min = PASSWORD_MIN_LENGTH) @MaxBytes(BCRYPT_MAX_PASSWORD_BYTES) String password,
        @NotBlank @Size(max = NAME_MAX_LENGTH) String name
) {

    public static final int EMAIL_MAX_LENGTH = 100;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
    public static final int NAME_MAX_LENGTH = 50;

    public SignupCommand toCommand() {
        return new SignupCommand(email, password, name.strip());
    }
}
