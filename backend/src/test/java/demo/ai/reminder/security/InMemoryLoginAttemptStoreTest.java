package demo.ai.reminder.security;

import demo.ai.reminder.support.MutableClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryLoginAttemptStoreTest extends LoginAttemptStoreContractTest {

    private static final LoginAttemptProperties PROPERTIES = new LoginAttemptProperties(
            5, 20, Duration.ofMinutes(15), Duration.ofMinutes(10), LoginAttemptProperties.Store.MEMORY);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-12T00:00:00Z"), ZoneOffset.UTC);
    private final InMemoryLoginAttemptStore store = new InMemoryLoginAttemptStore(PROPERTIES, clock);

    @Override
    LoginAttemptStore store() {
        return store;
    }

    @Override
    LoginAttemptProperties properties() {
        return PROPERTIES;
    }

    @Override
    void elapse(Duration duration) {
        clock.advance(duration);
    }

    @Test
    @DisplayName("남은 차단 시간은 주입한 Clock 기준으로 잠금 기간에서 흐른 시간만큼 줄어든다")
    void remainingLock_isMeasuredByClock() {
        failTimes(EMAIL, IP, 5);
        assertThat(store.remainingLock(EMAIL, IP)).contains(PROPERTIES.lockDuration());

        clock.advance(PROPERTIES.lockDuration().minusSeconds(1));

        assertThat(store.remainingLock(EMAIL, IP)).contains(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("기간이 지난 기록은 기록·조회할 때 정리되어 메모리에 남지 않는다")
    void expiredRecords_areRemoved() {
        failTimes(EMAIL, IP, 5);
        store.recordFailure("bob@example.com", "10.0.0.2");
        assertThat(store.trackedCount()).isEqualTo(4);

        clock.advance(PROPERTIES.window());
        store.remainingLock("carol@example.com", "10.0.0.3");

        assertThat(store.trackedCount()).isZero();
    }
}
