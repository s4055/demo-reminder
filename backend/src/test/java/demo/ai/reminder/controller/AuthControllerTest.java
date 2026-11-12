package demo.ai.reminder.controller;

import com.jayway.jsonpath.JsonPath;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.security.JwtProperties;
import demo.ai.reminder.security.JwtProvider;
import demo.ai.reminder.security.RefreshTokenCookie;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProperties jwtProperties;

    private record Tokens(String accessToken, Cookie refreshCookie) {

        String bearer() {
            return "Bearer " + accessToken;
        }
    }

    @Test
    @DisplayName("회원가입하면 201과 액세스 토큰, 사용자 정보를 반환하고 리프레시 토큰 쿠키를 발급한다")
    void signup_returnsAccessTokenAndUser_andSetsRefreshCookie() throws Exception {
        Tokens tokens = signup("Alice@Example.com", "password1", "앨리스");

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshCookie().getValue()).isNotBlank();
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, tokens.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.name").value("앨리스"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("이미 가입한 이메일(대소문자 무시)로 가입하면 409를 반환한다")
    void signup_withDuplicateEmail_returnsConflict() throws Exception {
        signup("alice@example.com", "password1", "앨리스");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("ALICE@example.com", "password2", "다른 앨리스")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("CONFLICT"));
    }

    @Test
    @DisplayName("이메일 형식이 틀리거나 비밀번호가 8자 미만이면 400을 반환한다")
    void signup_withInvalidFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("not-an-email", "short", "앨리스")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"));
        assertThat(userRepository.count()).isZero();
    }

    @Test
    @DisplayName("비밀번호는 UTF-8 72바이트까지 허용하고, 넘으면 400과 password 필드 오류를 반환한다 (BCrypt 제한)")
    void signup_withPasswordOver72Bytes_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("alice@example.com", "가".repeat(25), "앨리스")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.resultMsg").value("password: must be at most 72 bytes in UTF-8"));
        assertThat(userRepository.count()).isZero();

        signup("alice@example.com", "가".repeat(24), "앨리스");
    }

    @Test
    @DisplayName("가입할 때 이름의 앞뒤 공백은 지우고 저장한다")
    void signup_stripsName() throws Exception {
        Tokens tokens = signup("alice@example.com", "password1", "  앨리스  ");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, tokens.bearer()))
                .andExpect(jsonPath("$.data.name").value("앨리스"));
    }

    @Test
    @DisplayName("올바른 이메일/비밀번호로 로그인하면 액세스 토큰과 사용자 정보를 반환하고 Bearer 토큰으로 API를 호출할 수 있다")
    void login_withValidCredentials_returnsAccessToken() throws Exception {
        signup("alice@example.com", "password1", "앨리스");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ALICE@example.com\",\"password\":\"password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.id").isNumber())
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("앨리스"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andReturn();
        Tokens tokens = tokensOf(result);

        mockMvc.perform(get("/api/lists").header(HttpHeaders.AUTHORIZATION, tokens.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"));
    }

    @Test
    @DisplayName("비밀번호가 틀리거나 없는 이메일이면 같은 메시지로 401을 반환하고 리프레시 쿠키를 발급하지 않는다")
    void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
        signup("alice@example.com", "password1", "앨리스");

        for (String body : new String[]{
                "{\"email\":\"alice@example.com\",\"password\":\"wrong-password\"}",
                "{\"email\":\"nobody@example.com\",\"password\":\"password1\"}"}) {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.resultMsg").value("Invalid email or password"))
                    .andReturn();
            assertThat(result.getResponse().getCookie(RefreshTokenCookie.NAME)).isNull();
        }
    }

    @Test
    @DisplayName("같은 이메일로 5번 틀리면 올바른 비밀번호로도 429와 Retry-After 헤더를 반환하고, 다른 계정은 로그인할 수 있다")
    void login_afterFiveFailures_returnsTooManyRequestsWithRetryAfter() throws Exception {
        signup("blocked@example.com", "password1", "차단");
        signup("other@example.com", "password1", "다른 사용자");
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(loginRequest("blocked@example.com", "wrong-password", "10.2.0.1"))
                    .andExpect(status().isUnauthorized());
        }

        MvcResult blocked = mockMvc.perform(loginRequest("blocked@example.com", "password1", "10.2.0.1")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString(HttpHeaders.RETRY_AFTER)))
                .andExpect(jsonPath("$.resultCode").value("TOO_MANY_LOGIN_ATTEMPTS"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();
        assertThat(Long.parseLong(blocked.getResponse().getHeader(HttpHeaders.RETRY_AFTER))).isBetween(1L, 900L);
        assertThat(blocked.getResponse().getCookie(RefreshTokenCookie.NAME)).isNull();

        mockMvc.perform(loginRequest("other@example.com", "password1", "10.2.0.1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인과 인증된 요청은 세션을 만들지 않는다")
    void loginAndAuthenticatedRequest_doNotCreateSession() throws Exception {
        signup("alice@example.com", "password1", "앨리스");

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"password1\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult api = mockMvc.perform(get("/api/lists")
                        .header(HttpHeaders.AUTHORIZATION, tokensOf(login).bearer()))
                .andExpect(status().isOk())
                .andReturn();

        for (MvcResult result : new MvcResult[]{login, api}) {
            assertThat(result.getRequest().getSession(false)).isNull();
            assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                    .noneMatch(cookie -> cookie.startsWith("JSESSIONID="));
        }
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키는 HttpOnly, SameSite=Lax, Path=/api/auth와 리프레시 토큰 수명으로 발급된다")
    void refreshCookie_hasSecureAttributes() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("alice@example.com", "password1", "앨리스")))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .startsWith(RefreshTokenCookie.NAME + "=")
                .contains("HttpOnly", "SameSite=Lax", "Path=/api/auth",
                        "Max-Age=" + jwtProperties.refreshTokenTtl().toSeconds());
    }

    @Test
    @DisplayName("Bearer 토큰 없이 API를 호출하면 ApiResponse 형식의 401을 반환한다")
    void api_withoutToken_returnsUnauthorizedApiResponse() throws Exception {
        mockMvc.perform(get("/api/reminders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.resultMsg").value("로그인이 필요합니다."))
                .andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("만료, 위조, 형식 오류 토큰이나 Bearer가 아닌 인증 헤더로 API를 호출하면 401을 반환한다")
    void api_withInvalidToken_returnsUnauthorized() throws Exception {
        Tokens tokens = signup("alice@example.com", "password1", "앨리스");
        Long userId = JsonPath.<Integer>read(mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, tokens.bearer()))
                .andReturn().getResponse().getContentAsString(), "$.data.id").longValue();
        Instant past = Instant.now().minus(jwtProperties.accessTokenTtl()).minus(Duration.ofMinutes(1));
        String expired = new JwtProvider(jwtProperties, Clock.fixed(past, ZoneOffset.UTC)).issueAccessToken(userId);
        JwtProperties otherKey = new JwtProperties("another-secret-key-that-is-32-bytes!",
                jwtProperties.accessTokenTtl(), jwtProperties.refreshTokenTtl(), false);
        String forged = new JwtProvider(otherKey, Clock.systemUTC()).issueAccessToken(userId);

        for (String authorization : new String[]{
                "Bearer " + expired, "Bearer " + forged, "Bearer not-a-jwt", "Bearer ", tokens.accessToken(),
                "Basic " + tokens.accessToken()}) {
            mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, authorization))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"));
        }
    }

    @Test
    @DisplayName("교체 전 리프레시 쿠키로 재발급하면 401이고, 최신 리프레시 쿠키도 무효가 된다")
    void refresh_withReusedCookie_returnsUnauthorized_andInvalidatesLatestCookie() throws Exception {
        Tokens signedUp = signup("alice@example.com", "password1", "앨리스");
        Tokens latest = tokensOf(mockMvc.perform(post("/api/auth/refresh").cookie(signedUp.refreshCookie()))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(post("/api/auth/refresh").cookie(signedUp.refreshCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/auth/refresh").cookie(latest.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리프레시 쿠키로 재발급하면 새 액세스 토큰과 사용자 정보를 반환하고 쿠키를 교체한다")
    void refresh_returnsNewAccessToken_andRotatesCookie() throws Exception {
        Tokens signedUp = signup("alice@example.com", "password1", "앨리스");

        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(signedUp.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"))
                .andReturn();
        Tokens refreshed = tokensOf(result);

        assertThat(refreshed.refreshCookie().getValue()).isNotEqualTo(signedUp.refreshCookie().getValue());
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, refreshed.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/refresh").cookie(signedUp.refreshCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("리프레시 쿠키가 없거나 모르는 값이면 재발급은 401이다")
    void refresh_withoutValidCookie_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resultCode").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(RefreshTokenCookie.NAME, "unknown")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃하면 리프레시 쿠키를 지우고 그 리프레시 토큰으로는 재발급할 수 없다")
    void logout_revokesRefreshToken_andClearsCookie() throws Exception {
        Tokens tokens = signup("alice@example.com", "password1", "앨리스");

        mockMvc.perform(post("/api/auth/logout").cookie(tokens.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsStringIgnoringCase("Max-Age=0")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsStringIgnoringCase("Path=/api/auth")));

        mockMvc.perform(post("/api/auth/refresh").cookie(tokens.refreshCookie()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("리프레시 쿠키 없이 로그아웃해도 200을 반환한다")
    void logout_withoutCookie_succeeds() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("SUCCESS"));
    }

    @Test
    @DisplayName("두 사용자는 서로의 리스트를 볼 수 없고, 다른 사용자의 리스트를 수정하면 404다")
    void users_cannotSeeOrModifyEachOthersLists() throws Exception {
        String alice = signup("alice@example.com", "password1", "앨리스").bearer();
        String bob = signup("bob@example.com", "password1", "밥").bearer();
        String location = mockMvc.perform(post("/api/lists").header(HttpHeaders.AUTHORIZATION, alice)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"앨리스 장보기\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long aliceListId = Long.parseLong(location.replaceAll(".*\"data\":\\{\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/lists").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(put("/api/lists/{id}", aliceListId).header(HttpHeaders.AUTHORIZATION, bob)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"가로채기\",\"version\":0}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/lists").header(HttpHeaders.AUTHORIZATION, alice))
                .andExpect(jsonPath("$.data[0].name").value("앨리스 장보기"));
    }

    @Test
    @DisplayName("프론트엔드 출처의 preflight에는 Authorization 헤더와 리프레시 쿠키 전송을 허용한다")
    void corsPreflight_fromFrontend_allowsAuthorizationAndCredentials() throws Exception {
        mockMvc.perform(options("/api/reminders")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsStringIgnoringCase("authorization")));

        mockMvc.perform(options("/api/reminders")
                        .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private Tokens signup(String email, String password, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson(email, password, name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.email").value(email.toLowerCase()))
                .andReturn();
        return tokensOf(result);
    }

    private static Tokens tokensOf(MvcResult result) throws Exception {
        String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
        Cookie refreshCookie = result.getResponse().getCookie(RefreshTokenCookie.NAME);
        assertThat(refreshCookie).as("리프레시 토큰 쿠키").isNotNull();
        return new Tokens(accessToken, refreshCookie);
    }

    private static String signupJson(String email, String password, String name) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"" + name + "\"}";
    }

    private static MockHttpServletRequestBuilder loginRequest(String email, String password, String remoteAddr) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .with(request -> {
                    request.setRemoteAddr(remoteAddr);
                    return request;
                });
    }
}
