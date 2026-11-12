package demo.ai.reminder.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties("app.auth.login-attempt")
public record LoginAttemptProperties(int maxFailuresPerEmail, int maxFailuresPerIp, Duration window,
                                     Duration lockDuration) {

    public LoginAttemptProperties {
        if (maxFailuresPerEmail < 1 || maxFailuresPerIp < 1) {
            throw new IllegalArgumentException("app.auth.login-attempt max failures must be at least 1");
        }
        Objects.requireNonNull(window, "app.auth.login-attempt.window is required");
        Objects.requireNonNull(lockDuration, "app.auth.login-attempt.lock-duration is required");
    }
}
