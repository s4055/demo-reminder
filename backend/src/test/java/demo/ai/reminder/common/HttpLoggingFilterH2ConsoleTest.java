package demo.ai.reminder.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HttpLoggingFilter가 적용된 상태에서도 H2 콘솔에 로그인할 수 있는지 검증한다.
 * MockMvc의 Mock 요청은 실제 컨테이너처럼 폼 본문을 파라미터로 파싱하지 않아 문제를 재현하지 못하므로 내장 Tomcat으로 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
class HttpLoggingFilterH2ConsoleTest {

    private static final Pattern JSESSIONID = Pattern.compile("jsessionid=([0-9a-f]+)");

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("HttpLoggingFilter가 적용되어 있어도 H2 콘솔에 로그인할 수 있다")
    void h2ConsoleLoginSucceeds() throws Exception {
        HttpResponse<String> response = loginToH2Console();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .doesNotContain("No suitable driver")
                .contains("header.jsp", "tables.do");
    }

    @Test
    @DisplayName("H2 콘솔 요청은 요청/응답 로그를 남기지 않는다")
    void doesNotLogH2ConsoleRequests(CapturedOutput output) throws Exception {
        loginToH2Console();

        // 기동 로그에도 h2-console 경로가 찍힐 수 있으므로 경로가 아니라 필터의 로그 표식으로 확인한다.
        assertThat(output).doesNotContain("[Request]", "[Header]", "[Session]", "[Response]");
    }

    private HttpResponse<String> loginToH2Console() throws Exception {
        String loginPage = httpClient.send(
                HttpRequest.newBuilder(uri("/h2-console/login.jsp")).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        Matcher matcher = JSESSIONID.matcher(loginPage);
        assertThat(matcher.find()).as("H2 콘솔 로그인 페이지의 jsessionid").isTrue();

        String form = "driver=" + encode("org.h2.Driver")
                + "&url=" + encode("jdbc:h2:mem:reminderdb")
                + "&user=" + encode("sa")
                + "&password=";
        return httpClient.send(
                HttpRequest.newBuilder(uri("/h2-console/login.do?jsessionid=" + matcher.group(1)))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
