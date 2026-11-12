package demo.ai.reminder.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

abstract class LoginAttemptStoreContractTest {

    static final String EMAIL = "alice@example.com";
    static final String IP = "10.0.0.1";

    abstract LoginAttemptStore store();

    abstract LoginAttemptProperties properties();

    abstract void elapse(Duration duration);

    @Test
    @DisplayName("같은 이메일로 한도보다 1번 적게 실패하면 차단하지 않고, 한도에 닿으면 다른 IP에서도 그 이메일을 차단한다")
    void recordFailure_locksEmailAtMaxFailures() {
        failTimes(EMAIL, IP, properties().maxFailuresPerEmail() - 1);
        assertThat(store().remainingLock(EMAIL, IP)).isEmpty();

        store().recordFailure(EMAIL, IP);

        assertThat(store().remainingLock(EMAIL, "10.0.0.2")).hasValueSatisfying(remaining ->
                assertThat(remaining).isPositive().isLessThanOrEqualTo(properties().lockDuration()));
    }

    @Test
    @DisplayName("이메일은 대소문자와 앞뒤 공백을 무시하고 같은 이메일로 센다")
    void recordFailure_normalizesEmail() {
        failTimes(" Alice@Example.com ", IP, properties().maxFailuresPerEmail() - 1);
        store().recordFailure("ALICE@example.com", IP);

        assertThat(store().remainingLock(EMAIL, IP)).isPresent();
    }

    @Test
    @DisplayName("잠금 기간이 지나면 차단이 풀리고, 다시 실패하면 처음부터 센다")
    void remainingLock_releasesAfterLockDuration() {
        failTimes(EMAIL, IP, properties().maxFailuresPerEmail());
        assertThat(store().remainingLock(EMAIL, IP)).isPresent();

        elapse(properties().lockDuration());
        assertThat(store().remainingLock(EMAIL, IP)).isEmpty();

        store().recordFailure(EMAIL, IP);
        assertThat(store().remainingLock(EMAIL, IP)).isEmpty();
    }

    @Test
    @DisplayName("기록 기간이 지난 실패는 세지 않는다")
    void recordFailure_ignoresFailuresOutsideWindow() {
        failTimes(EMAIL, IP, properties().maxFailuresPerEmail() - 1);
        elapse(properties().window());

        store().recordFailure(EMAIL, IP);

        assertThat(store().remainingLock(EMAIL, IP)).isEmpty();
    }

    @Test
    @DisplayName("로그인에 성공해 초기화하면 이메일 실패 횟수를 처음부터 다시 센다")
    void reset_clearsEmailFailures() {
        failTimes(EMAIL, IP, properties().maxFailuresPerEmail() - 1);

        store().reset(EMAIL);
        failTimes(EMAIL, IP, properties().maxFailuresPerEmail() - 1);

        assertThat(store().remainingLock(EMAIL, IP)).isEmpty();
    }

    @Test
    @DisplayName("같은 IP에서 여러 이메일로 IP 한도만큼 실패하면 그 IP의 모든 로그인을 차단한다")
    void recordFailure_locksIpAtMaxFailures() {
        for (int i = 0; i < properties().maxFailuresPerIp(); i++) {
            store().recordFailure("user" + i + "@example.com", IP);
        }

        assertThat(store().remainingLock("new@example.com", IP)).isPresent();
        assertThat(store().remainingLock("new@example.com", "10.0.0.2")).isEmpty();
    }

    @Test
    @DisplayName("다른 이메일의 실패는 이메일 기준 차단에 섞이지 않는다")
    void recordFailure_keepsEmailsSeparate() {
        failTimes(EMAIL, IP, properties().maxFailuresPerEmail() - 1);
        failTimes("bob@example.com", IP, properties().maxFailuresPerEmail() - 1);

        assertThat(store().remainingLock(EMAIL, IP)).isEmpty();
        assertThat(store().remainingLock("bob@example.com", IP)).isEmpty();
    }

    void failTimes(String email, String ip, int times) {
        for (int i = 0; i < times; i++) {
            store().recordFailure(email, ip);
        }
    }
}
