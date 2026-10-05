package demo.ai.reminder.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Email @Size(max = 100) String email,
        // BCrypt는 72바이트까지만 쓰므로 바이트 길이는 서비스에서 한 번 더 확인한다.
        @NotBlank @Size(min = PASSWORD_MIN_LENGTH, max = 72) String password,
        @NotBlank @Size(max = 50) String name
) {

    public static final int PASSWORD_MIN_LENGTH = 8;
}
