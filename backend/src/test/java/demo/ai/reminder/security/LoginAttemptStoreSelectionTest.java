package demo.ai.reminder.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptStoreSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(StoreConfig.class)
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(StringRedisTemplate.class, () -> new StringRedisTemplate(new LettuceConnectionFactory()))
            .withPropertyValues(
                    "app.auth.login-attempt.max-failures-per-email=5",
                    "app.auth.login-attempt.max-failures-per-ip=20",
                    "app.auth.login-attempt.window=15m",
                    "app.auth.login-attempt.lock-duration=15m");

    @Test
    @DisplayName("store를 지정하지 않으면 메모리 저장소를 쓴다")
    void defaultStore_isInMemory() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(LoginAttemptStore.class);
            assertThat(context.getBean(LoginAttemptStore.class)).isInstanceOf(InMemoryLoginAttemptStore.class);
            assertThat(context.getBean(LoginAttemptProperties.class).store()).isEqualTo(LoginAttemptProperties.Store.MEMORY);
        });
    }

    @Test
    @DisplayName("store=memory이면 메모리 저장소를, store=redis이면 Redis 저장소를 쓴다")
    void configuredStore_isSelected() {
        runner.withPropertyValues("app.auth.login-attempt.store=memory").run(context ->
                assertThat(context.getBean(LoginAttemptStore.class)).isInstanceOf(InMemoryLoginAttemptStore.class));
        runner.withPropertyValues("app.auth.login-attempt.store=redis").run(context -> {
            assertThat(context).hasSingleBean(LoginAttemptStore.class);
            assertThat(context.getBean(LoginAttemptStore.class)).isInstanceOf(RedisLoginAttemptStore.class);
        });
    }

    @Configuration
    @EnableConfigurationProperties(LoginAttemptProperties.class)
    @Import({InMemoryLoginAttemptStore.class, RedisLoginAttemptStore.class})
    static class StoreConfig {
    }
}
