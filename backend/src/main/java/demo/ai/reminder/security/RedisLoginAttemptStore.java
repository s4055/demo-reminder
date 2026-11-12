package demo.ai.reminder.security;

import demo.ai.reminder.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.auth.login-attempt", name = "store", havingValue = "redis")
@RequiredArgsConstructor
public class RedisLoginAttemptStore implements LoginAttemptStore {

    private static final String FAILURE_KEY_PREFIX = "login:fail:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";
    private static final long LOCK_STARTED = 1L;
    private static final RedisScript<Long> RECORD_FAILURE = RedisScript.of("""
            if redis.call('EXISTS', KEYS[2]) == 1 then
                return 0
            end
            local failures = redis.call('INCR', KEYS[1])
            if failures == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            if failures >= tonumber(ARGV[1]) then
                redis.call('SET', KEYS[2], '1', 'PX', ARGV[3])
                redis.call('DEL', KEYS[1])
                return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final LoginAttemptProperties properties;

    @Override
    public Optional<Duration> remainingLock(String email, String ip) {
        try {
            return Stream.of(lockKey(Target.EMAIL, User.normalizeEmail(email)), lockKey(Target.IP, ip))
                    .map(key -> redisTemplate.getExpire(key, TimeUnit.MILLISECONDS))
                    .filter(Objects::nonNull)
                    .filter(ttl -> ttl > 0)
                    .map(Duration::ofMillis)
                    .max(Duration::compareTo);
        } catch (DataAccessException e) {
            log.error("Failed to read login lock from Redis; allowing login attempt", e);
            return Optional.empty();
        }
    }

    @Override
    public void recordFailure(String email, String ip) {
        String normalizedEmail = User.normalizeEmail(email);
        try {
            if (recordFailure(Target.EMAIL, normalizedEmail, properties.maxFailuresPerEmail())) {
                log.warn("Login locked for email={} ip={} for={}", normalizedEmail, ip, properties.lockDuration());
            }
            if (recordFailure(Target.IP, ip, properties.maxFailuresPerIp())) {
                log.warn("Login locked for ip={} email={} for={}", ip, normalizedEmail, properties.lockDuration());
            }
        } catch (DataAccessException e) {
            log.error("Failed to record login failure in Redis: email={} ip={}", normalizedEmail, ip, e);
        }
    }

    @Override
    public void reset(String email) {
        try {
            redisTemplate.delete(failureKey(Target.EMAIL, User.normalizeEmail(email)));
        } catch (DataAccessException e) {
            log.error("Failed to reset login failures in Redis", e);
        }
    }

    private boolean recordFailure(Target target, String value, int maxFailures) {
        Long result = redisTemplate.execute(RECORD_FAILURE,
                List.of(failureKey(target, value), lockKey(target, value)),
                String.valueOf(maxFailures),
                String.valueOf(properties.window().toMillis()),
                String.valueOf(properties.lockDuration().toMillis()));
        return Objects.equals(result, LOCK_STARTED);
    }

    static String failureKey(Target target, String value) {
        return FAILURE_KEY_PREFIX + target.key + ":" + value;
    }

    static String lockKey(Target target, String value) {
        return LOCK_KEY_PREFIX + target.key + ":" + value;
    }

    enum Target {
        EMAIL("email"),
        IP("ip");

        private final String key;

        Target(String key) {
            this.key = key;
        }
    }
}
