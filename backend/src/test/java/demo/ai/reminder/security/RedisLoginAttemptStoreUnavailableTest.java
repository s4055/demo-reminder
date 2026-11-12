package demo.ai.reminder.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(OutputCaptureExtension.class)
class RedisLoginAttemptStoreUnavailableTest {

    private static final LoginAttemptProperties PROPERTIES = new LoginAttemptProperties(
            5, 20, Duration.ofMinutes(15), Duration.ofMinutes(15), LoginAttemptProperties.Store.REDIS);

    private LettuceConnectionFactory connectionFactory;
    private RedisLoginAttemptStore store;

    @BeforeEach
    void connectToClosedPort() throws IOException {
        LettuceClientConfiguration client = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(500))
                .build();
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", unusedPort()), client);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        store = new RedisLoginAttemptStore(new StringRedisTemplate(connectionFactory), PROPERTIES);
    }

    @AfterEach
    void close() {
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("Redis에 연결할 수 없으면 차단하지 않고 로그인을 허용하며 ERROR 로그를 남긴다")
    void redisUnavailable_failsOpenAndLogsError(CapturedOutput output) {
        assertThat(store.remainingLock("alice@example.com", "10.0.0.1")).isEmpty();
        assertThatCode(() -> store.recordFailure("alice@example.com", "10.0.0.1")).doesNotThrowAnyException();
        assertThatCode(() -> store.reset("alice@example.com")).doesNotThrowAnyException();

        assertThat(output.getOut())
                .contains("Failed to read login lock from Redis")
                .contains("Failed to record login failure in Redis")
                .contains("Failed to reset login failures in Redis");
    }

    private static int unusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
