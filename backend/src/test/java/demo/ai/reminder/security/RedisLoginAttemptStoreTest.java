package demo.ai.reminder.security;

import demo.ai.reminder.security.RedisLoginAttemptStore.Target;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisLoginAttemptStoreTest extends LoginAttemptStoreContractTest {

    private static final LoginAttemptProperties PROPERTIES = new LoginAttemptProperties(
            5, 20, Duration.ofMillis(1500), Duration.ofMillis(800), LoginAttemptProperties.Store.REDIS);
    private static final Duration TTL_MARGIN = Duration.ofMillis(150);

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    private static LettuceConnectionFactory serverA;
    private static LettuceConnectionFactory serverB;
    private static StringRedisTemplate templateA;
    private static StringRedisTemplate templateB;

    private RedisLoginAttemptStore store;

    @BeforeAll
    static void connect() {
        serverA = connectionFactory();
        serverB = connectionFactory();
        templateA = new StringRedisTemplate(serverA);
        templateB = new StringRedisTemplate(serverB);
    }

    @AfterAll
    static void disconnect() {
        serverA.destroy();
        serverB.destroy();
    }

    @BeforeEach
    void flush() {
        templateA.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
        store = new RedisLoginAttemptStore(templateA, PROPERTIES);
    }

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
        try {
            Thread.sleep(duration.plus(TTL_MARGIN).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("같은 Redis를 쓰는 두 서버에 실패를 나눠 기록하면 합산되어, 두 서버 모두에서 차단된다")
    void recordFailure_isSharedBetweenServers() {
        RedisLoginAttemptStore otherServer = new RedisLoginAttemptStore(templateB, PROPERTIES);
        for (int i = 0; i < 4; i++) {
            (i % 2 == 0 ? store : otherServer).recordFailure(EMAIL, IP);
        }
        assertThat(store.remainingLock(EMAIL, IP)).isEmpty();
        assertThat(otherServer.remainingLock(EMAIL, IP)).isEmpty();

        otherServer.recordFailure(EMAIL, IP);

        assertThat(store.remainingLock(EMAIL, IP)).isPresent();
        assertThat(otherServer.remainingLock(EMAIL, IP)).isPresent();
    }

    @Test
    @DisplayName("한 서버에서 로그인에 성공해 초기화하면 다른 서버에서도 그 이메일의 실패 횟수가 사라진다")
    void reset_isSharedBetweenServers() {
        RedisLoginAttemptStore otherServer = new RedisLoginAttemptStore(templateB, PROPERTIES);
        failTimes(EMAIL, IP, 4);

        otherServer.reset(EMAIL);
        store.recordFailure(EMAIL, IP);

        assertThat(otherServer.remainingLock(EMAIL, IP)).isEmpty();
    }

    @Test
    @DisplayName("여러 서버에서 동시에 실패를 기록해도 횟수가 유실되지 않는다")
    void recordFailure_countsConcurrentFailuresExactly() throws Exception {
        LoginAttemptProperties highLimit = new LoginAttemptProperties(
                1000, 1000, Duration.ofMinutes(1), Duration.ofMinutes(1), LoginAttemptProperties.Store.REDIS);
        List<RedisLoginAttemptStore> servers = List.of(
                new RedisLoginAttemptStore(templateA, highLimit), new RedisLoginAttemptStore(templateB, highLimit));

        try (ExecutorService executor = Executors.newFixedThreadPool(16)) {
            List<Future<?>> futures = IntStream.range(0, 200)
                    .<Future<?>>mapToObj(i -> executor.submit(() -> servers.get(i % 2).recordFailure(EMAIL, IP)))
                    .toList();
            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertThat(templateA.opsForValue().get(RedisLoginAttemptStore.failureKey(Target.EMAIL, EMAIL))).isEqualTo("200");
        assertThat(templateA.opsForValue().get(RedisLoginAttemptStore.failureKey(Target.IP, IP))).isEqualTo("200");
    }

    @Test
    @DisplayName("기록과 차단 키에는 기간과 잠금 기간만큼 TTL이 걸려 Redis가 만료시킨다")
    void keys_expireByTtl() {
        store.recordFailure(EMAIL, IP);
        failTimes("bob@example.com", IP, 5);

        assertThat(templateA.getExpire(RedisLoginAttemptStore.failureKey(Target.EMAIL, EMAIL)))
                .isPositive().isLessThanOrEqualTo(PROPERTIES.window().toSeconds() + 1);
        assertThat(templateA.hasKey(RedisLoginAttemptStore.lockKey(Target.EMAIL, "bob@example.com"))).isTrue();
        assertThat(templateA.hasKey(RedisLoginAttemptStore.failureKey(Target.EMAIL, "bob@example.com"))).isFalse();

        elapse(PROPERTIES.window());

        assertThat(templateA.keys("login:*")).isEmpty();
    }

    private static LettuceConnectionFactory connectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }
}
