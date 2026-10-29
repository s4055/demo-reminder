package demo.ai.reminder.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.h2.console.path=/custom-h2")
@ExtendWith(OutputCaptureExtension.class)
class HttpLoggingFilterH2ConsolePathTest {

    @Autowired
    private HttpLoggingFilter httpLoggingFilter;

    @Test
    @DisplayName("spring.h2.console.path로 바꾼 H2 콘솔 경로의 요청은 로깅하지 않고 그대로 넘긴다")
    void passesConfiguredH2ConsolePathThroughWithoutLogging(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/custom-h2/login.do");
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<Object> passed = new ArrayList<>();
        FilterChain chain = (req, res) -> {
            passed.add(req);
            passed.add(res);
        };

        httpLoggingFilter.doFilter(request, response, chain);

        assertThat(passed).containsExactly(request, response);
        assertThat(output).doesNotContain("[Request]", "[Response]");
    }

    @Test
    @DisplayName("H2 콘솔 경로를 바꾸면 기본 경로 /h2-console 요청은 일반 요청처럼 로깅한다")
    void logsDefaultH2ConsolePathWhenPathIsChanged(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/h2-console/login.jsp");

        httpLoggingFilter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(output).contains("[Request] GET /h2-console/login.jsp");
    }
}
