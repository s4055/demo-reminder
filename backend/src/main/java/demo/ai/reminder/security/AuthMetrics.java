package demo.ai.reminder.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    public static final String LOGIN_FAILURES = "reminder.auth.login.failures";
    public static final String LOGIN_LOCKS = "reminder.auth.login.locks";
    public static final String LOGIN_REJECTED = "reminder.auth.login.rejected";
    public static final String REFRESH_REUSE = "reminder.auth.refresh.reuse";
    public static final String LOGIN_ATTEMPT_STORE_ERRORS = "reminder.auth.login-attempt.store.errors";
    public static final String SCOPE = "scope";

    private final Counter loginFailures;
    private final Counter emailLocks;
    private final Counter ipLocks;
    private final Counter loginRejected;
    private final Counter refreshReuse;
    private final Counter loginAttemptStoreErrors;

    public AuthMetrics(MeterRegistry registry) {
        this.loginFailures = Counter.builder(LOGIN_FAILURES)
                .description("Login attempts rejected for invalid email or password")
                .register(registry);
        this.emailLocks = lockCounter(registry, "email");
        this.ipLocks = lockCounter(registry, "ip");
        this.loginRejected = Counter.builder(LOGIN_REJECTED)
                .description("Login attempts rejected with 429 while locked")
                .register(registry);
        this.refreshReuse = Counter.builder(REFRESH_REUSE)
                .description("Reuse of revoked refresh tokens")
                .register(registry);
        this.loginAttemptStoreErrors = Counter.builder(LOGIN_ATTEMPT_STORE_ERRORS)
                .description("Login attempts allowed without a lock check because the store failed")
                .register(registry);
    }

    private static Counter lockCounter(MeterRegistry registry, String scope) {
        return Counter.builder(LOGIN_LOCKS)
                .description("Login locks started")
                .tag(SCOPE, scope)
                .register(registry);
    }

    public void loginFailed() {
        loginFailures.increment();
    }

    public void emailLocked() {
        emailLocks.increment();
    }

    public void ipLocked() {
        ipLocks.increment();
    }

    public void loginRejected() {
        loginRejected.increment();
    }

    public void refreshTokenReused() {
        refreshReuse.increment();
    }

    public void loginAttemptStoreFailedOpen() {
        loginAttemptStoreErrors.increment();
    }
}
