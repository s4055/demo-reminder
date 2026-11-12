package demo.ai.reminder.security;

import java.time.Duration;
import java.util.Optional;

public interface LoginAttemptStore {

    Optional<Duration> remainingLock(String email, String ip);

    void recordFailure(String email, String ip);

    void reset(String email);
}
