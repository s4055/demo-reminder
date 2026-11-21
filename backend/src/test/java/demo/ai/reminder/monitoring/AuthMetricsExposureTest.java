package demo.ai.reminder.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
class AuthMetricsExposureTest {

    private static final String EMAIL = "metrics-exposure@example.com";
    private static final Pattern TAG_KEY = Pattern.compile("([a-z_]+)=\"");

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("인증·보안 카운터는 기동 직후부터 prometheus에 노출된다")
    void authCounters_areRegisteredAtStartup() throws Exception {
        assertThat(scrape()).contains(
                "reminder_auth_login_failures_total{application=\"reminder\"}",
                "reminder_auth_login_locks_total{application=\"reminder\",scope=\"email\"}",
                "reminder_auth_login_locks_total{application=\"reminder\",scope=\"ip\"}",
                "reminder_auth_login_rejected_total{application=\"reminder\"}",
                "reminder_auth_refresh_reuse_total{application=\"reminder\"}",
                "reminder_auth_login_attempt_store_errors_total{application=\"reminder\"}");
    }

    @Test
    @DisplayName("로그인 실패 후에도 인증·보안 메트릭에는 이메일과 IP가 없고 태그는 application과 scope뿐이다")
    void authMetrics_doNotExposeEmailOrIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(EMAIL, "wrong-password");
        }

        List<String> authLines = scrape().lines().filter(line -> line.startsWith("reminder_auth_")).toList();

        assertThat(authLines).isNotEmpty().allSatisfy(line -> {
            assertThat(line).doesNotContain(EMAIL, "@", "127.0.0.1", "0:0:0:0");
            Matcher keys = TAG_KEY.matcher(line);
            while (keys.find()) {
                assertThat(keys.group(1)).isIn("application", "scope");
            }
        });
    }

    private void login(String email, String password) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + serverPort + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private String scrape() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + managementPort + "/actuator/prometheus"))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
