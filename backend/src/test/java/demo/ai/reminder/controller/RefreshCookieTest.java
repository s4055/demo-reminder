package demo.ai.reminder.controller;

import demo.ai.reminder.repository.user.RefreshTokenRepository;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.security.RefreshTokenCookie;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RefreshCookieTest {

    private static final String EMAIL = "cookie-test@example.com";
    private static final String PASSWORD = "password1";

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @AfterEach
    void deleteUser() {
        refreshTokenRepository.deleteAll();
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("회원가입 응답은 HttpOnly, SameSite=Lax 리프레시 쿠키만 발급하고 JSESSIONID는 발급하지 않는다")
    void signup_issuesRefreshCookieWithoutSessionCookie() throws Exception {
        HttpResponse<String> response = signup();

        assertThat(response.statusCode()).isEqualTo(201);
        assertRefreshCookieOnly(response);
    }

    @Test
    @DisplayName("로그인 응답은 HttpOnly, SameSite=Lax 리프레시 쿠키만 발급하고 JSESSIONID는 발급하지 않는다")
    void login_issuesRefreshCookieWithoutSessionCookie() throws Exception {
        signup();

        HttpResponse<String> response = post("/api/auth/login",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertRefreshCookieOnly(response);
    }

    private HttpResponse<String> signup() throws Exception {
        return post("/api/auth/signup",
                "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\",\"name\":\"쿠키\"}");
    }

    private static void assertRefreshCookieOnly(HttpResponse<String> response) {
        List<String> cookies = response.headers().allValues("Set-Cookie");
        assertThat(cookies).noneMatch(cookie -> cookie.startsWith("JSESSIONID="));
        assertThat(cookies).singleElement().satisfies(cookie -> assertThat(cookie)
                .startsWith(RefreshTokenCookie.NAME + "=")
                .containsIgnoringCase("HttpOnly")
                .containsIgnoringCase("SameSite=Lax")
                .contains("Path=/api/auth"));
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
