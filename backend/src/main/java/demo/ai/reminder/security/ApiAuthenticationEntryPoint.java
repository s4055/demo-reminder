package demo.ai.reminder.security;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 로그인하지 않은 요청을 401로 응답한다. 보안 필터에서 막히므로 GlobalExceptionHandler를 거치지 않아
 * 여기서 직접 ApiResponse 형식으로 본문을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ResultCode resultCode = ResultCode.UNAUTHORIZED;
        response.setStatus(resultCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(
                ApiResponse.error(resultCode, resultCode.getDefaultMessage())));
    }
}
