package demo.ai.reminder.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "request_id";

    // "password": "..." 형태 (이스케이프된 따옴표 포함)
    private static final Pattern PASSWORD_FIELD = Pattern.compile("(\"password\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"");

    private static final String MASK = "****";

    private final String h2ConsolePath;

    public HttpLoggingFilter(@Value("${spring.h2.console.path:/h2-console}") String h2ConsolePath) {
        this.h2ConsolePath = h2ConsolePath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // H2 콘솔은 로그인 값을 폼 파라미터로 읽는데, 본문을 미리 읽으면 파라미터가 비어 로그인할 수 없으므로 로깅하지 않고 그대로 넘긴다.
        if (isH2ConsoleRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        try {
            CachedBodyRequestWrapper cachedRequest = new CachedBodyRequestWrapper(request);
            log.info("[Request] {} {} body={}", request.getMethod(), request.getRequestURI(),
                    maskPassword(new String(cachedRequest.getBody(), StandardCharsets.UTF_8)));
            log.info("[Header] {}", requestHeaders(request));
            log.info("[Session] {}", sessionInfo(request));

            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            try {
                filterChain.doFilter(cachedRequest, wrappedResponse);
            } finally {
                log.info("[Response] status={} body={}", wrappedResponse.getStatus(),
                        new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8));
                wrappedResponse.copyBodyToResponse();
            }
        } finally {
            // 요청 스레드는 스레드 풀에서 재사용되므로 다음 요청에 request_id가 남지 않도록 반드시 지운다.
            MDC.remove(REQUEST_ID);
        }
    }

    static String maskPassword(String body) {
        return PASSWORD_FIELD.matcher(body).replaceAll("$1\"" + MASK + "\"");
    }

    private boolean isH2ConsoleRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals(h2ConsolePath) || path.startsWith(h2ConsolePath + "/");
    }

    private String requestHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .map(name -> name + "=" + String.join(", ", Collections.list(request.getHeaders(name))))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * 세션 ID와 속성을 헤더와 같은 [key=value, ...] 형식으로 남긴다.
     * 로깅 때문에 세션이 새로 생기지 않도록 기존 세션만 조회하고, 세션이 없으면 none을 남긴다.
     */
    private String sessionInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "none";
        }
        return Stream.concat(
                        Stream.of("id=" + session.getId()),
                        Collections.list(session.getAttributeNames()).stream()
                                .map(name -> name + "=" + session.getAttribute(name)))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
