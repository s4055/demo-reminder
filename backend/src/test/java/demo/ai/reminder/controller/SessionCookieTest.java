package demo.ai.reminder.controller;

import demo.ai.reminder.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 세션 쿠키 속성(SameSite, HttpOnly)을 검증한다.
 * server.servlet.session.cookie 설정은 내장 Tomcat이 적용하고 MockMvc는 세션 쿠키를 발급하지 않으므로 실제 서버로 확인한다.
 * 다른 테스트와 in-memory DB를 공유하고 트랜잭션 롤백도 되지 않으므로 만든 사용자는 테스트 후 지운다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionCookieTest {

    private static final String EMAIL = "cookie-test@example.com";
    private static final String PASSWORD = "password1";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @AfterEach
    void deleteUser() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("회원가입 응답의 세션 쿠키는 SameSite=Lax, HttpOnly로 발급된다")
    void signup_issuesSessionCookieWithSameSiteLaxAndHttpOnly() throws Exception {
        HttpResponse<String> response = post("/api/auth/signup",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\",\"name\":\"쿠키\"}");

        assertThat(response.statusCode()).isEqualTo(201);
        assertSessionCookieAttributes(response);
    }

    @Test
    @DisplayName("로그인 응답의 세션 쿠키는 SameSite=Lax, HttpOnly로 발급된다")
    void login_issuesSessionCookieWithSameSiteLaxAndHttpOnly() throws Exception {
        post("/api/auth/signup",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\",\"name\":\"쿠키\"}");

        HttpResponse<String> response = post("/api/auth/login",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertSessionCookieAttributes(response);
    }

    private static void assertSessionCookieAttributes(HttpResponse<String> response) {
        String sessionCookie = response.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("JSESSIONID="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("JSESSIONID Set-Cookie 헤더가 없다: " + response.headers()));
        assertThat(sessionCookie)
                .containsIgnoringCase("SameSite=Lax")
                .containsIgnoringCase("HttpOnly");
    }

    private HttpResponse<String> post(String path, String json) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
