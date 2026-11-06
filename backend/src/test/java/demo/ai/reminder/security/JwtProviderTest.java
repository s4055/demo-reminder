package demo.ai.reminder.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-jwt-secret-key-at-least-32-bytes";
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Instant NOW = Instant.parse("2026-11-06T03:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private static JwtProvider providerAt(Instant instant) {
        return providerAt(instant, SECRET);
    }

    private static JwtProvider providerAt(Instant instant, String secret) {
        JwtProperties properties = new JwtProperties(secret, ACCESS_TTL, Duration.ofDays(14), false);
        return new JwtProvider(properties, Clock.fixed(instant, ZONE));
    }

    @Test
    @DisplayName("발급한 액세스 토큰을 검증하면 사용자 ID를 돌려준다")
    void verifyAccessToken_returnsUserIdOfIssuedToken() {
        JwtProvider provider = providerAt(NOW);

        String token = provider.issueAccessToken(42L);

        assertThat(provider.verifyAccessToken(token)).contains(42L);
    }

    @Test
    @DisplayName("액세스 토큰은 sub에 사용자 ID를, 주입한 Clock 기준으로 발급·만료 시각을 담는다")
    void issueAccessToken_containsSubjectAndClockBasedTimes() {
        String token = providerAt(NOW).issueAccessToken(42L);

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> Date.from(NOW))
                .build()
                .parseSignedClaims(token);

        assertThat(claims.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getPayload().getSubject()).isEqualTo("42");
        assertThat(claims.getPayload().getIssuedAt()).isEqualTo(Date.from(NOW));
        assertThat(claims.getPayload().getExpiration()).isEqualTo(Date.from(NOW.plus(ACCESS_TTL)));
    }

    @Test
    @DisplayName("만료 직전까지는 유효하고 만료 시각이 지나면 거부한다")
    void verifyAccessToken_rejectsExpiredToken() {
        String token = providerAt(NOW).issueAccessToken(42L);

        assertThat(providerAt(NOW.plus(ACCESS_TTL).minusSeconds(1)).verifyAccessToken(token)).contains(42L);
        assertThat(providerAt(NOW.plus(ACCESS_TTL).plusSeconds(1)).verifyAccessToken(token)).isEmpty();
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰은 거부한다")
    void verifyAccessToken_rejectsTokenSignedWithOtherKey() {
        String forged = providerAt(NOW, "another-secret-key-that-is-32-bytes!").issueAccessToken(42L);

        assertThat(providerAt(NOW).verifyAccessToken(forged)).isEmpty();
    }

    @Test
    @DisplayName("페이로드를 바꾼 토큰은 거부한다")
    void verifyAccessToken_rejectsTamperedPayload() {
        String[] parts = providerAt(NOW).issueAccessToken(42L).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"42\"", "\"1\"");
        String tampered = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        assertThat(providerAt(NOW).verifyAccessToken(tampered)).isEmpty();
    }

    @Test
    @DisplayName("서명 없는(alg=none) 토큰은 거부한다")
    void verifyAccessToken_rejectsUnsignedToken() {
        String unsigned = Jwts.builder()
                .subject("42")
                .issuedAt(Date.from(NOW))
                .expiration(Date.from(NOW.plus(ACCESS_TTL)))
                .compact();

        assertThat(providerAt(NOW).verifyAccessToken(unsigned)).isEmpty();
    }

    @Test
    @DisplayName("형식이 잘못됐거나 비어 있는 토큰은 거부한다")
    void verifyAccessToken_rejectsMalformedToken() {
        JwtProvider provider = providerAt(NOW);

        assertThat(provider.verifyAccessToken("not-a-jwt")).isEmpty();
        assertThat(provider.verifyAccessToken("a.b.c")).isEmpty();
        assertThat(provider.verifyAccessToken("")).isEmpty();
        assertThat(provider.verifyAccessToken(" ")).isEmpty();
        assertThat(provider.verifyAccessToken(null)).isEmpty();
    }

    @Test
    @DisplayName("sub가 숫자가 아닌 토큰은 거부한다")
    void verifyAccessToken_rejectsNonNumericSubject() {
        String token = Jwts.builder()
                .subject("alice")
                .issuedAt(Date.from(NOW))
                .expiration(Date.from(NOW.plus(ACCESS_TTL)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThat(providerAt(NOW).verifyAccessToken(token)).isEmpty();
    }
}
