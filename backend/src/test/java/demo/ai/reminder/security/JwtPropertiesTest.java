package demo.ai.reminder.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtPropertiesConfig.class)
            .withPropertyValues("app.jwt.access-token-ttl=15m", "app.jwt.refresh-token-ttl=14d");

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtPropertiesConfig {
    }

    @Test
    @DisplayName("서명 키가 32바이트 이상이면 설정이 만들어진다")
    void create_withSecretOf32Bytes_succeeds() {
        JwtProperties properties = new JwtProperties("a".repeat(32), ACCESS_TTL, REFRESH_TTL, false);

        assertThat(properties.secretBytes()).hasSize(32);
    }

    @Test
    @DisplayName("서명 키가 32바이트 미만이거나 없으면 예외가 발생한다")
    void create_withShortOrMissingSecret_throws() {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(31), ACCESS_TTL, REFRESH_TTL, false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(null, ACCESS_TTL, REFRESH_TTL, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("서명 키 길이는 글자 수가 아니라 UTF-8 바이트 수로 판단한다")
    void create_countsSecretLengthInUtf8Bytes() {
        assertThat(new JwtProperties("가".repeat(11), ACCESS_TTL, REFRESH_TTL, false).secretBytes()).hasSize(33);
        assertThatThrownBy(() -> new JwtProperties("가".repeat(10), ACCESS_TTL, REFRESH_TTL, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("문자열 표현에 서명 키가 드러나지 않는다")
    void toString_hidesSecret() {
        String secret = "s".repeat(32);

        assertThat(new JwtProperties(secret, ACCESS_TTL, REFRESH_TTL, false).toString()).doesNotContain(secret);
    }

    @Test
    @DisplayName("서명 키가 32바이트 미만이면 애플리케이션 시작이 실패한다")
    void startup_failsWithShortSecret() {
        contextRunner.withPropertyValues("app.jwt.secret=" + "a".repeat(31))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("서명 키가 32바이트 이상이면 설정 값을 바인딩한다")
    void startup_bindsPropertiesWithValidSecret() {
        contextRunner.withPropertyValues("app.jwt.secret=" + "a".repeat(32), "app.jwt.cookie-secure=true")
                .run(context -> {
                    JwtProperties properties = context.getBean(JwtProperties.class);
                    assertThat(properties.accessTokenTtl()).isEqualTo(ACCESS_TTL);
                    assertThat(properties.refreshTokenTtl()).isEqualTo(REFRESH_TTL);
                    assertThat(properties.cookieSecure()).isTrue();
                });
    }
}
