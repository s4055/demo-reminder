package demo.ai.reminder.monitoring;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0",
        "app.auth.login-attempt.store=redis",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=1",
        "spring.data.redis.timeout=500ms",
        "spring.data.redis.connect-timeout=500ms"})
class RedisHealthTest {

    @LocalManagementPort
    private int managementPort;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("store=redis이면 health에 Redis가 포함되지만, Redis가 내려가도 readiness는 UP이다")
    void redisStore_redisDown_readinessStillUp() throws Exception {
        HttpResponse<String> health = get("/actuator/health");
        HttpResponse<String> readiness = get("/actuator/health/readiness");

        assertThat(JsonPath.<String>read(health.body(), "$.components.redis.status")).isEqualTo("DOWN");
        assertThat(readiness.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(readiness.body(), "$.status")).isEqualTo("UP");
        assertThat(JsonPath.<Map<String, Object>>read(readiness.body(), "$.components")).doesNotContainKey("redis");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + managementPort + path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
