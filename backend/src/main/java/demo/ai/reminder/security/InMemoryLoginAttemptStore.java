package demo.ai.reminder.security;

import demo.ai.reminder.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.auth.login-attempt", name = "store", havingValue = "memory", matchIfMissing = true)
@RequiredArgsConstructor
public class InMemoryLoginAttemptStore implements LoginAttemptStore {

    private final Map<String, Attempts> emailAttempts = new ConcurrentHashMap<>();
    private final Map<String, Attempts> ipAttempts = new ConcurrentHashMap<>();
    private final LoginAttemptProperties properties;
    private final Clock clock;

    @Override
    public Optional<Duration> remainingLock(String email, String ip) {
        Instant now = Instant.now(clock);
        removeExpired(now);
        return Stream.of(emailAttempts.get(User.normalizeEmail(email)), ipAttempts.get(ip))
                .flatMap(attempts -> attempts == null ? Stream.empty() : attempts.remainingLock(now).stream())
                .max(Duration::compareTo);
    }

    @Override
    public void recordFailure(String email, String ip) {
        Instant now = Instant.now(clock);
        removeExpired(now);
        String normalizedEmail = User.normalizeEmail(email);
        if (recordFailure(emailAttempts, normalizedEmail, properties.maxFailuresPerEmail(), now)) {
            log.warn("Login locked for email={} ip={} until={}", normalizedEmail, ip, now.plus(properties.lockDuration()));
        }
        if (recordFailure(ipAttempts, ip, properties.maxFailuresPerIp(), now)) {
            log.warn("Login locked for ip={} email={} until={}", ip, normalizedEmail, now.plus(properties.lockDuration()));
        }
    }

    @Override
    public void reset(String email) {
        emailAttempts.remove(User.normalizeEmail(email));
    }

    int trackedCount() {
        return emailAttempts.size() + ipAttempts.size();
    }

    private boolean recordFailure(Map<String, Attempts> attempts, String key, int maxFailures, Instant now) {
        Attempts updated = attempts.compute(key, (ignored, current) ->
                (current == null || current.isExpired(now, properties.window()) ? Attempts.first(now) : current)
                        .withFailure(now, maxFailures, properties.lockDuration()));
        return updated.justLocked(now);
    }

    private void removeExpired(Instant now) {
        emailAttempts.values().removeIf(attempts -> attempts.isExpired(now, properties.window()));
        ipAttempts.values().removeIf(attempts -> attempts.isExpired(now, properties.window()));
    }

    private record Attempts(int failures, Instant windowStart, Instant lockedUntil, Instant lockedAt) {

        static Attempts first(Instant now) {
            return new Attempts(0, now, null, null);
        }

        Attempts withFailure(Instant now, int maxFailures, Duration lockDuration) {
            int count = failures + 1;
            if (count >= maxFailures) {
                return new Attempts(count, windowStart, now.plus(lockDuration), now);
            }
            return new Attempts(count, windowStart, null, null);
        }

        boolean justLocked(Instant now) {
            return now.equals(lockedAt);
        }

        Optional<Duration> remainingLock(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil)
                    ? Optional.of(Duration.between(now, lockedUntil))
                    : Optional.empty();
        }

        boolean isExpired(Instant now, Duration window) {
            Instant expiresAt = lockedUntil == null ? windowStart.plus(window) : lockedUntil;
            return !now.isBefore(expiresAt);
        }
    }
}
