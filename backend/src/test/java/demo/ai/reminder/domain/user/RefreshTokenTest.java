package demo.ai.reminder.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static demo.ai.reminder.support.TestTimes.seoul;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshTokenTest {

    private static final Instant NOW = seoul(2026, 11, 6, 12, 0);
    private static final Instant EXPIRES_AT = NOW.plus(Duration.ofDays(14));

    private final User user = new User("alice@example.com", "password", "앨리스");

    private RefreshToken newToken() {
        return new RefreshToken(user, "hash", EXPIRES_AT);
    }

    @Test
    @DisplayName("만료 시각 전에는 만료되지 않고 만료 시각부터 만료로 판단한다")
    void isExpired_fromExpiresAt() {
        RefreshToken token = newToken();

        assertThat(token.isExpired(EXPIRES_AT.minusSeconds(1))).isFalse();
        assertThat(token.isExpired(EXPIRES_AT)).isTrue();
        assertThat(token.isExpired(EXPIRES_AT.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName("새 토큰은 폐기되지 않았고 만료 전이면 사용할 수 있다")
    void isUsable_whenNotRevokedAndNotExpired() {
        RefreshToken token = newToken();

        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isUsable(NOW)).isTrue();
        assertThat(token.isUsable(EXPIRES_AT)).isFalse();
    }

    @Test
    @DisplayName("폐기하면 폐기 시각이 기록되고 더 이상 사용할 수 없다")
    void revoke_marksRevoked() {
        RefreshToken token = newToken();

        token.revoke(NOW);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(NOW);
        assertThat(token.isUsable(NOW)).isFalse();
    }

    @Test
    @DisplayName("이미 폐기된 토큰을 다시 폐기해도 처음 폐기 시각을 유지한다")
    void revoke_keepsFirstRevokedAt() {
        RefreshToken token = newToken();
        token.revoke(NOW);

        token.revoke(NOW.plus(Duration.ofHours(1)));

        assertThat(token.getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("회전하면 기존 토큰은 폐기되고 같은 사용자의 새 토큰이 만들어진다")
    void rotate_revokesCurrentAndCreatesNewToken() {
        RefreshToken token = newToken();
        Instant newExpiresAt = NOW.plus(Duration.ofDays(14)).plus(Duration.ofHours(1));

        RefreshToken rotated = token.rotate("new-hash", newExpiresAt, NOW);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(NOW);
        assertThat(rotated.getUser()).isSameAs(user);
        assertThat(rotated.getTokenHash()).isEqualTo("new-hash");
        assertThat(rotated.getExpiresAt()).isEqualTo(newExpiresAt);
        assertThat(rotated.isUsable(NOW)).isTrue();
    }

    @Test
    @DisplayName("폐기되었거나 만료된 토큰은 회전할 수 없다")
    void rotate_throwsWhenRevokedOrExpired() {
        RefreshToken revoked = newToken();
        revoked.revoke(NOW);
        RefreshToken expired = newToken();

        assertThatThrownBy(() -> revoked.rotate("new-hash", EXPIRES_AT, NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> expired.rotate("new-hash", EXPIRES_AT.plus(Duration.ofDays(14)), EXPIRES_AT))
                .isInstanceOf(IllegalStateException.class);
    }
}
