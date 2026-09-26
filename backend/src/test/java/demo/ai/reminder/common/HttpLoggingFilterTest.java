package demo.ai.reminder.common;

import jakarta.servlet.FilterChain;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
    @DisplayName("요청 헤더와 세션은 [Request] 다음에 [Header], [Session] 로그로 따로 남기고 응답 로그에는 남기지 않는다")
    void logsHeaderAndSessionAsSeparateLinesAfterRequest(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/lists").header("X-Client-Version", "1.2.0"))
                .andExpect(status().isOk());

        String log = output.getOut();
        int requestLog = log.indexOf("- [Request] GET /api/lists body=");
        int headerLog = log.indexOf("- [Header] [", requestLog);
        int sessionLog = log.indexOf("- [Session] ", headerLog);
        int responseLog = log.indexOf("- [Response] status=200 body=", sessionLog);
        assertThat(requestLog).isNotNegative();
        assertThat(headerLog).isGreaterThan(requestLog);
        assertThat(sessionLog).isGreaterThan(headerLog);
        assertThat(responseLog).isGreaterThan(sessionLog);
        assertThat(output).containsPattern("\\[Header] \\[[^]]*X-Client-Version=1\\.2\\.0]");

        List<String> requestIds = requestIdsOf(output, "[Request]", "[Header]", "[Session]", "[Response]");
        assertThat(requestIds).hasSize(4);
        assertThat(requestIds).containsOnly(requestIds.get(0));
    }

    @Test
    @DisplayName("세션이 있으면 세션 ID와 속성을 [key=value, ...] 형식으로, 없으면 none을 남긴다")
    void logsSessionInfo(CapturedOutput output) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", "tester");
        session.setAttribute("theme", "dark");

        mockMvc.perform(get("/api/lists").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/api/lists")).andExpect(status().isOk());

        assertThat(output).contains("[Session] [id=" + session.getId() + ", ");
        assertThat(output).containsPattern("\\[Session] \\[id=[^]]*loginUser=tester");
        assertThat(output).containsPattern("\\[Session] \\[id=[^]]*theme=dark");
        assertThat(output).contains("[Session] none");
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

    /** 주어진 문구가 들어간 로그 줄에서, 로그 패턴이 앞에 붙인 request_id를 순서대로 뽑는다. */
    private List<String> requestIdsOf(CapturedOutput output, String... markers) {
        return output.getOut().lines()
                .filter(line -> Arrays.stream(markers).anyMatch(line::contains))
                .map(REQUEST_ID_PATTERN::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1))
                .toList();
    }
}
