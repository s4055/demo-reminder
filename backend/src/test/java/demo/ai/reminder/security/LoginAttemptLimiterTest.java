package demo.ai.reminder.security;

import demo.ai.reminder.support.MutableClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptLimiterTest {

    private static final String EMAIL = "alice@example.com";
    private static final String IP = "10.0.0.1";
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(10);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-12T00:00:00Z"), ZoneOffset.UTC);
    private final LoginAttemptLimiter limiter =
            new LoginAttemptLimiter(new LoginAttemptProperties(5, 20, WINDOW, LOCK), clock);

    @Test
    @DisplayName("같은 이메일로 4번 실패하면 아직 차단하지 않고, 5번째 실패부터 잠금 기간만큼 차단한다")
    void recordFailure_locksEmailAfterFiveFailures() {
        failTimes(EMAIL, IP, 4);
        assertThat(limiter.remainingLock(EMAIL, IP)).isEmpty();

        limiter.recordFailure(EMAIL, IP);

        assertThat(limiter.remainingLock(EMAIL, "10.0.0.2")).contains(LOCK);
    }

    @Test
    @DisplayName("이메일은 대소문자와 앞뒤 공백을 무시하고 같은 이메일로 센다")
    void recordFailure_normalizesEmail() {
        failTimes(" Alice@Example.com ", IP, 4);
        limiter.recordFailure("ALICE@example.com", IP);

        assertThat(limiter.remainingLock(EMAIL, IP)).isPresent();
    }

    @Test
    @DisplayName("잠금 기간이 지나면 차단이 풀리고, 다시 실패하면 처음부터 센다")
    void remainingLock_releasesAfterLockDuration() {
        failTimes(EMAIL, IP, 5);
        clock.advance(LOCK.minusSeconds(1));
        assertThat(limiter.remainingLock(EMAIL, IP)).contains(Duration.ofSeconds(1));

        clock.advance(Duration.ofSeconds(1));
        assertThat(limiter.remainingLock(EMAIL, IP)).isEmpty();

        limiter.recordFailure(EMAIL, IP);
        assertThat(limiter.remainingLock(EMAIL, IP)).isEmpty();
    }

    @Test
    @DisplayName("기록 기간이 지난 실패는 세지 않는다")
    void recordFailure_ignoresFailuresOutsideWindow() {
        failTimes(EMAIL, IP, 4);
        clock.advance(WINDOW);

        limiter.recordFailure(EMAIL, IP);

        assertThat(limiter.remainingLock(EMAIL, IP)).isEmpty();
    }

    @Test
    @DisplayName("로그인에 성공해 초기화하면 이메일 실패 횟수가 처음부터 다시 센다")
    void reset_clearsEmailFailures() {
        failTimes(EMAIL, IP, 4);

        limiter.reset(EMAIL);
        failTimes(EMAIL, IP, 4);

        assertThat(limiter.remainingLock(EMAIL, IP)).isEmpty();
    }

    @Test
    @DisplayName("같은 IP에서 여러 이메일로 20번 실패하면 그 IP의 모든 로그인을 차단한다")
    void recordFailure_locksIpAfterTwentyFailures() {
        for (int i = 0; i < 20; i++) {
            limiter.recordFailure("user" + i + "@example.com", IP);
        }

        assertThat(limiter.remainingLock("new@example.com", IP)).contains(LOCK);
        assertThat(limiter.remainingLock("new@example.com", "10.0.0.2")).isEmpty();
    }

    @Test
    @DisplayName("다른 이메일의 실패는 이메일 기준 차단에 섞이지 않는다")
    void recordFailure_keepsEmailsSeparate() {
        failTimes(EMAIL, IP, 4);
        failTimes("bob@example.com", IP, 4);

        assertThat(limiter.remainingLock(EMAIL, IP)).isEmpty();
        assertThat(limiter.remainingLock("bob@example.com", IP)).isEmpty();
    }

    @Test
    @DisplayName("기간이 지난 기록은 기록·조회할 때 정리되어 메모리에 남지 않는다")
    void expiredRecords_areRemoved() {
        failTimes(EMAIL, IP, 5);
        limiter.recordFailure("bob@example.com", "10.0.0.2");
        assertThat(limiter.trackedCount()).isEqualTo(4);

        clock.advance(WINDOW);
        limiter.remainingLock("carol@example.com", "10.0.0.3");

        assertThat(limiter.trackedCount()).isZero();
    }

    private void failTimes(String email, String ip, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(email, ip);
        }
    }
}
