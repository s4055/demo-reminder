package demo.ai.reminder.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.logging.http.excluded-paths=/actuator,/h2-console")
@ExtendWith(OutputCaptureExtension.class)
class HttpLoggingFilterExcludedPathsTest {

    @Autowired
    private HttpLoggingFilter httpLoggingFilter;

    @Test
    @DisplayName("제외 경로로 설정한 모든 경로의 요청은 로깅하지 않고 원래 요청을 그대로 넘긴다")
    void passesEveryExcludedPathThroughWithoutLogging(CapturedOutput output) throws Exception {
        for (String uri : List.of("/actuator", "/actuator/health", "/h2-console/login.do")) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
            MockHttpServletResponse response = new MockHttpServletResponse();
            List<Object> passed = new ArrayList<>();
            FilterChain chain = (req, res) -> {
                passed.add(req);
                passed.add(res);
            };

            httpLoggingFilter.doFilter(request, response, chain);

            assertThat(passed).containsExactly(request, response);
        }
        assertThat(output).doesNotContain("[Request]", "[Response]");
        assertThat(MDC.get(HttpLoggingFilter.REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("제외 경로가 아니거나 경로 문자열로만 시작하는 요청은 로깅한다")
    void logsRequestsOutsideExcludedPaths(CapturedOutput output) throws Exception {
        for (String uri : List.of("/api/lists", "/actuator-backup")) {
            httpLoggingFilter.doFilter(new MockHttpServletRequest("GET", uri), new MockHttpServletResponse(),
                    (req, res) -> { });
        }

        assertThat(output).contains("[Request] GET /api/lists", "[Request] GET /actuator-backup");
    }

    @Test
    @DisplayName("컨텍스트 경로를 뺀 경로로 제외 여부를 판단한다")
    void matchesExcludedPathWithoutContextPath(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/actuator/health");
        request.setContextPath("/app");

        httpLoggingFilter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(output).doesNotContain("[Request]");
    }
}
