package demo.ai.reminder.logging;

import com.jayway.jsonpath.JsonPath;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.security.RefreshTokenCookie;
import demo.ai.reminder.support.TestAuth;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class HttpLoggingFilterTest {

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("\\[([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})] ");

    private final Logger businessLog = LoggerFactory.getLogger("demo.ai.reminder.service.BusinessLogic");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HttpLoggingFilter httpLoggingFilter;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void signIn() {
        owner = TestAuth.signIn(userRepository, "owner@example.com");
    }

    @Test
    @DisplayName("요청과 응답의 전문을 로그로 남기고 응답 본문은 그대로 전달한다")
    void logsRequestAndResponse(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"장보기\",\"color\":\"#FF9500\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("장보기"));

        assertThat(output).contains("[Request]", "POST /api/lists", "{\"name\":\"장보기\",\"color\":\"#FF9500\"}");
        assertThat(output).contains("[Response]", "status=201", "\"resultCode\":\"SUCCESS\"");
    }

    @Test
    @DisplayName("요청 헤더는 [Request] 다음에 [Header] 로그로 따로 남기고, 세션 로그는 남기지 않는다")
    void logsHeaderAsSeparateLineAfterRequest_withoutSessionLog(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/lists").header("X-Client-Version", "1.2.0"))
                .andExpect(status().isOk());

        String log = output.getOut();
        int requestLog = log.indexOf("- [Request] GET /api/lists body=");
        int headerLog = log.indexOf("- [Header] [", requestLog);
        int responseLog = log.indexOf("- [Response] status=200 body=", headerLog);
        assertThat(requestLog).isNotNegative();
        assertThat(headerLog).isGreaterThan(requestLog);
        assertThat(responseLog).isGreaterThan(headerLog);
        assertThat(output).containsPattern("\\[Header] \\[[^]]*X-Client-Version=1\\.2\\.0]");
        assertThat(output).doesNotContain("[Session]");

        List<String> requestIds = requestIdsOf(output, "[Request]", "[Header]", "[Response]");
        assertThat(requestIds).hasSize(3);
        assertThat(requestIds).containsOnly(requestIds.get(0));
    }

    @Test
    @DisplayName("Cookie와 Authorization 헤더 값은 가리고 다른 헤더는 그대로 남긴다")
    void masksCookieAndAuthorizationHeaders(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/lists")
                        .header("Cookie", "JSESSIONID=secret-session-value")
                        .header("Authorization", "Bearer secret-token")
                        .header("X-Client-Version", "1.2.0"))
                .andExpect(status().isOk());

        assertThat(output).containsPattern("\\[Header] \\[[^]]*(?i:cookie)=\\*\\*\\*\\*");
        assertThat(output).containsPattern("\\[Header] \\[[^]]*(?i:authorization)=\\*\\*\\*\\*");
        assertThat(output).containsPattern("\\[Header] \\[[^]]*X-Client-Version=1\\.2\\.0");
        assertThat(output).doesNotContain("secret-session-value", "secret-token");
    }

    @Test
    @DisplayName("로그인과 재발급 응답의 액세스 토큰과 리프레시 토큰 값은 로그에 남지 않는다")
    void doesNotLogTokensOfLoginAndRefresh(CapturedOutput output) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"password1\",\"name\":\"앨리스\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie refreshCookie = login.getResponse().getCookie(RefreshTokenCookie.NAME);
        MvcResult refresh = mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andReturn();

        for (MvcResult result : new MvcResult[]{login, refresh}) {
            String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
            String refreshToken = result.getResponse().getCookie(RefreshTokenCookie.NAME).getValue();
            assertThat(output).doesNotContain(accessToken, refreshToken);
        }
        assertThat(output).contains("\"accessToken\":\"****\"", "\"email\":\"alice@example.com\"");
    }

    @Test
    @DisplayName("로그인 요청 본문의 비밀번호는 로그에 가려서 남긴다")
    void masksPasswordInRequestLog(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"s3cret-\\\"pw\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(output).contains("\"email\":\"nobody@example.com\",\"password\":\"****\"");
        assertThat(output).doesNotContain("s3cret");
    }

    @Test
    @DisplayName("JSON의 password와 accessToken 값만 가리고 다른 필드는 그대로 둔다")
    void maskSecrets_replacesOnlyPasswordAndAccessTokenValues() {
        assertThat(HttpLoggingFilter.maskSecrets("{\"name\":\"a\", \"password\" : \"x\\\"y\", \"memo\":\"password\"}"))
                .isEqualTo("{\"name\":\"a\", \"password\" : \"****\", \"memo\":\"password\"}");
        assertThat(HttpLoggingFilter.maskSecrets("{\"data\":{\"accessToken\":\"eyJ.abc.def\",\"user\":{\"id\":1}}}"))
                .isEqualTo("{\"data\":{\"accessToken\":\"****\",\"user\":{\"id\":1}}}");
    }

    @Test
    @DisplayName("요청 로그, 비즈니스 로직, 응답 로그 순서로 출력한다")
    void logsRequestBeforeBusinessLogicAndResponseAfter(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"work\"}"))
                .andExpect(status().isCreated());

        String log = output.getOut();
        int requestLog = log.indexOf("[Request]");
        int businessLog = log.indexOf("insert", requestLog);
        int responseLog = log.indexOf("[Response]", businessLog);
        assertThat(requestLog).isNotNegative();
        assertThat(businessLog).isGreaterThan(requestLog);
        assertThat(responseLog).isGreaterThan(businessLog);
    }

    @Test
    @DisplayName("한 요청의 요청/응답 로그는 같은 UUID request_id를, 다른 요청은 다른 request_id를 갖는다")
    void usesSameUuidRequestIdWithinRequestAndDifferentAcrossRequests(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/lists")).andExpect(status().isOk());
        mockMvc.perform(get("/api/lists")).andExpect(status().isOk());

        List<String> requestIds = requestIdsOf(output, "[Request]", "[Response]");
        assertThat(requestIds).hasSize(4);
        assertThat(requestIds.get(0)).isEqualTo(requestIds.get(1));
        assertThat(requestIds.get(2)).isEqualTo(requestIds.get(3));
        assertThat(requestIds.get(0)).isNotEqualTo(requestIds.get(2));
    }

    @Test
    @DisplayName("요청을 처리하는 동안 남긴 로그에도 필터와 같은 request_id가 찍히고, 처리가 끝나면 MDC에서 지워진다")
    void sharesRequestIdWithBusinessLogicLogs(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lists");
        FilterChain businessLogic = (req, res) -> businessLog.info("business logic");

        httpLoggingFilter.doFilter(request, new MockHttpServletResponse(), businessLogic);

        List<String> requestIds = requestIdsOf(output, "[Request]", "business logic", "[Response]");
        assertThat(requestIds).hasSize(3);
        assertThat(requestIds).containsOnly(requestIds.get(0));
        assertThat(MDC.get(HttpLoggingFilter.REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("H2 콘솔 요청은 로깅하지 않고 원래 요청을 그대로 다음 체인에 넘긴다")
    void passesH2ConsoleRequestThroughWithoutLogging(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/h2-console/login.do");
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<Object> passed = new ArrayList<>();
        FilterChain chain = (req, res) -> {
            passed.add(req);
            passed.add(res);
        };

        httpLoggingFilter.doFilter(request, response, chain);

        assertThat(passed).containsExactly(request, response);
        assertThat(output).doesNotContain("[Request]", "[Response]");
        assertThat(MDC.get(HttpLoggingFilter.REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("경로가 /h2-console로 시작하기만 하는 다른 요청은 로깅한다")
    void logsRequestWhosePathOnlyStartsWithH2ConsoleText(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/h2-console-backup");

        httpLoggingFilter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(output).contains("[Request] GET /h2-console-backup");
    }

    @Test
    @DisplayName("요청을 처리하며 실행한 Hibernate SQL 로그에도 같은 request_id가 찍힌다")
    void sharesRequestIdWithHibernateSqlLogs(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"work\"}"))
                .andExpect(status().isCreated());

        List<String> requestIds = requestIdsOf(output, "[Request]", "[SQL:", "[Response]");
        assertThat(requestIds).hasSizeGreaterThanOrEqualTo(3);
        assertThat(requestIds).containsOnly(requestIds.get(0));
    }

    @Test
    @DisplayName("로그는 [시각] [레벨] [스레드] [로거:라인] [request_id] - 메시지 형식으로 출력한다")
    void printsLogInConfiguredPattern(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/lists")).andExpect(status().isOk());

        assertThat(output.getOut()).containsPattern(
                "\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}] \\[INFO ] \\[[^\\]]+] "
                        + "\\[HttpLoggingFilter:\\d+] " + REQUEST_ID_PATTERN.pattern() + "- \\[Request] GET /api/lists");
    }

    @Test
    @DisplayName("요청 밖에서 남긴 로그는 request_id 자리에 startup을 출력한다")
    void printsStartupWhenNoRequestId(CapturedOutput output) {
        businessLog.info("outside request");

        assertThat(output.getOut()).contains("[startup] - outside request");
    }

    private List<String> requestIdsOf(CapturedOutput output, String... markers) {
        return output.getOut().lines()
                .filter(line -> Arrays.stream(markers).anyMatch(line::contains))
                .map(REQUEST_ID_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .toList();
    }
}
