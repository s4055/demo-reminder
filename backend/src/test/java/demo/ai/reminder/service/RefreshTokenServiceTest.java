package demo.ai.reminder.service;

import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.user.RefreshToken;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.repository.user.RefreshTokenRepository;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.security.JwtProperties;
import demo.ai.reminder.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class RefreshTokenServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 12, 0);
    private static final Instant NOW_INSTANT = NOW.atZone(ZONE).toInstant();

    @TestConfiguration
    static class MutableClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(NOW_INSTANT, ZONE);
        }
    }

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MutableClock clock;

    private User alice;

    @BeforeEach
    void setUp() {
        clock.setInstant(NOW_INSTANT);
        alice = userRepository.save(new User("alice@example.com", "{noop}password", "앨리스"));
    }

    private static String sha256Hex(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private RefreshToken stored(String token) throws Exception {
        return refreshTokenRepository.findByTokenHash(sha256Hex(token)).orElseThrow();
    }

    private void assertUnauthorized(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("발급하면 원문이 아닌 SHA-256 해시를 수명만큼의 만료 시각과 함께 저장한다")
    void issue_storesHashOnly() throws Exception {
        IssuedRefreshToken issued = refreshTokenService.issue(alice.getId());

        assertThat(issued.userId()).isEqualTo(alice.getId());
        assertThat(refreshTokenRepository.findAll())
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.getTokenHash()).isNotEqualTo(issued.token())
                            .isEqualTo(sha256Hex(issued.token()));
                    assertThat(token.getUser().getId()).isEqualTo(alice.getId());
                    assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(jwtProperties.refreshTokenTtl()));
                    assertThat(token.isRevoked()).isFalse();
                });
    }

    @Test
    @DisplayName("발급할 때마다 서로 다른 무작위 토큰을 만든다")
    void issue_generatesDistinctRandomTokens() {
        String first = refreshTokenService.issue(alice.getId()).token();
        String second = refreshTokenService.issue(alice.getId()).token();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    @DisplayName("발급 결과의 문자열 표현에 토큰 원문이 드러나지 않는다")
    void issuedRefreshToken_toStringHidesToken() {
        IssuedRefreshToken issued = refreshTokenService.issue(alice.getId());

        assertThat(issued.toString()).doesNotContain(issued.token());
    }

    @Test
    @DisplayName("회전하면 기존 토큰은 폐기되고 새 토큰이 새 만료 시각으로 저장된다")
    void rotate_revokesCurrentAndStoresNewToken() throws Exception {
        String original = refreshTokenService.issue(alice.getId()).token();
        clock.advance(Duration.ofHours(1));

        IssuedRefreshToken rotated = refreshTokenService.rotate(original);

        assertThat(rotated.userId()).isEqualTo(alice.getId());
        assertThat(rotated.token()).isNotEqualTo(original);
        assertThat(stored(original).getRevokedAt()).isEqualTo(NOW.plusHours(1));
        RefreshToken newToken = stored(rotated.token());
        assertThat(newToken.isRevoked()).isFalse();
        assertThat(newToken.getExpiresAt()).isEqualTo(NOW.plusHours(1).plus(jwtProperties.refreshTokenTtl()));
    }

    @Test
    @DisplayName("회전한 새 토큰으로 다시 회전할 수 있다")
    void rotate_canChainWithNewToken() {
        String original = refreshTokenService.issue(alice.getId()).token();
        String second = refreshTokenService.rotate(original).token();

        IssuedRefreshToken third = refreshTokenService.rotate(second);

        assertThat(third.userId()).isEqualTo(alice.getId());
    }

    @Test
    @DisplayName("이미 회전한 토큰으로 다시 회전하면 401 예외가 발생한다")
    void rotate_throwsUnauthorized_whenTokenAlreadyRotated() {
        String original = refreshTokenService.issue(alice.getId()).token();
        refreshTokenService.rotate(original);

        assertUnauthorized(() -> refreshTokenService.rotate(original));
    }

    @Test
    @DisplayName("저장되지 않았거나 비어 있는 토큰으로 회전하면 401 예외가 발생한다")
    void rotate_throwsUnauthorized_whenTokenUnknownOrBlank() {
        assertUnauthorized(() -> refreshTokenService.rotate("unknown-token"));
        assertUnauthorized(() -> refreshTokenService.rotate(""));
        assertUnauthorized(() -> refreshTokenService.rotate(null));
    }

    @Test
    @DisplayName("만료 직전까지는 회전할 수 있고 만료 시각이 되면 401 예외가 발생한다")
    void rotate_throwsUnauthorized_whenTokenExpired() {
        String beforeExpiry = refreshTokenService.issue(alice.getId()).token();
        String expired = refreshTokenService.issue(alice.getId()).token();

        clock.advance(jwtProperties.refreshTokenTtl().minusSeconds(1));
        assertThatCode(() -> refreshTokenService.rotate(beforeExpiry)).doesNotThrowAnyException();

        clock.advance(Duration.ofSeconds(1));
        assertUnauthorized(() -> refreshTokenService.rotate(expired));
    }

    @Test
    @DisplayName("폐기한 토큰은 폐기 시각이 기록되고 회전할 수 없다")
    void revoke_preventsRotation() throws Exception {
        String token = refreshTokenService.issue(alice.getId()).token();

        refreshTokenService.revoke(token);

        assertThat(stored(token).getRevokedAt()).isEqualTo(NOW);
        assertUnauthorized(() -> refreshTokenService.rotate(token));
    }

    @Test
    @DisplayName("저장되지 않았거나 비어 있는 토큰을 폐기해도 예외 없이 넘어간다")
    void revoke_ignoresUnknownOrBlankToken() {
        assertThatCode(() -> {
            refreshTokenService.revoke("unknown-token");
            refreshTokenService.revoke("");
            refreshTokenService.revoke(null);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("토큰 해시는 중복 저장할 수 없다")
    void tokenHash_isUnique() {
        refreshTokenRepository.saveAndFlush(new RefreshToken(alice, "same-hash", NOW.plusDays(1)));

        assertThatThrownBy(() -> refreshTokenRepository.saveAndFlush(new RefreshToken(alice, "same-hash", NOW.plusDays(1))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
