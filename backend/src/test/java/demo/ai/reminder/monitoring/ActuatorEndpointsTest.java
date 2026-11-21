package demo.ai.reminder.monitoring;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
@ExtendWith(OutputCaptureExtension.class)
class ActuatorEndpointsTest {

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("management 포트의 health, liveness, readiness는 인증 없이 200으로 응답한다")
    void healthProbes_withoutAuthentication_returnOk() throws Exception {
        for (String path : new String[]{"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}) {
            HttpResponse<String> response = get(managementPort, path);

            assertThat(response.statusCode()).as(path).isEqualTo(200);
            assertThat(JsonPath.<String>read(response.body(), "$.status")).as(path).isEqualTo("UP");
        }
    }

    @Test
    @DisplayName("readiness에는 DB가 포함되고, 구성 요소의 세부 값은 보여 주지 않는다")
    void readiness_includesDbWithoutDetails() throws Exception {
        String body = get(managementPort, "/actuator/health/readiness").body();

        Map<String, Object> components = JsonPath.read(body, "$.components");
        assertThat(components).containsOnlyKeys("db", "readinessState");
        assertThat(JsonPath.<Map<String, Object>>read(body, "$.components.db")).containsOnlyKeys("status");
    }

    @Test
    @DisplayName("store=memory에서는 Redis health를 만들지 않고 Redis 없이도 readiness가 UP이다")
    void memoryStore_readinessUpWithoutRedis() throws Exception {
        String health = get(managementPort, "/actuator/health").body();
        String readiness = get(managementPort, "/actuator/health/readiness").body();

        assertThat(JsonPath.<Map<String, Object>>read(health, "$.components")).doesNotContainKey("redis");
        assertThat(JsonPath.<String>read(readiness, "$.status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("info에 애플리케이션 이름과 빌드 버전이 있다")
    void info_containsBuildNameAndVersion() throws Exception {
        HttpResponse<String> response = get(managementPort, "/actuator/info");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonPath.<String>read(response.body(), "$.build.name")).isEqualTo("reminder");
        assertThat(JsonPath.<String>read(response.body(), "$.build.version")).isNotBlank();
    }

    @Test
    @DisplayName("prometheus에 HTTP·JVM·HikariCP 메트릭과 application 태그, HTTP 요청 히스토그램이 있다")
    void prometheus_exposesHttpJvmAndHikariMetricsWithApplicationTag() throws Exception {
        get(serverPort, "/api/lists");

        HttpResponse<String> response = get(managementPort, "/actuator/prometheus");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("http_server_requests_seconds_count{", "http_server_requests_seconds_bucket{")
                .contains("jvm_memory_used_bytes{", "hikaricp_connections{")
                .contains("application=\"reminder\"");
    }

    @Test
    @DisplayName("노출하지 않은 Actuator 엔드포인트는 404로 응답한다")
    void unexposedEndpoints_returnNotFound() throws Exception {
        for (String path : new String[]{"/actuator/env", "/actuator/beans", "/actuator/metrics", "/actuator/loggers"}) {
            assertThat(get(managementPort, path).statusCode()).as(path).isEqualTo(404);
        }
    }

    @Test
    @DisplayName("앱 포트에서는 Actuator 경로가 응답하지 않는다")
    void appPort_doesNotServeActuator() throws Exception {
        for (String path : new String[]{"/actuator/health", "/actuator/prometheus", "/actuator/info"}) {
            assertThat(get(serverPort, path).statusCode()).as(path).isEqualTo(404);
        }
    }

    @Test
    @DisplayName("Actuator 요청은 요청 로그에 남지 않는다")
    void actuatorRequests_areNotLogged(CapturedOutput output) throws Exception {
        get(managementPort, "/actuator/health");
        get(managementPort, "/actuator/prometheus");

        assertThat(output).doesNotContain("[Request] GET /actuator");
    }

    private HttpResponse<String> get(int port, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
