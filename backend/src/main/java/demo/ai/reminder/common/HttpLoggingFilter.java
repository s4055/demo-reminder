package demo.ai.reminder.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 요청과 응답의 전문을 요청 로그 → 비즈니스 로직 → 응답 로그 순서로 남기고,
 * 모든 로그를 UUID 기반 request_id로 묶어 하나의 요청으로 구분할 수 있게 한다.
 * <ul>
 *   <li>요청 로그: {@code [Request]} 메서드, URI, 본문 / {@code [Header]} 요청 헤더 / {@code [Session]} 세션 정보</li>
 *   <li>응답 로그: {@code [Response]} 상태 코드, 본문</li>
 * </ul>
 * <p>
 * request_id는 MDC({@value #REQUEST_ID})에 넣어 두므로 요청을 처리하는 동안 서비스 등에서 남기는 로그에도 같은 값이 찍힌다.
 * 출력 형식은 application.yml의 {@code logging.pattern.console}에서 정한다.
 * <p>
 * 요청 로그를 체인 실행 전에 남기기 위해 요청 본문은 {@link CachedBodyRequestWrapper}로 미리 읽어 두고,
 * 캐싱된 응답 본문은 {@code copyBodyToResponse()}로 반드시 클라이언트에 돌려준다.
 * <p>
 * {@code /h2-console} 요청은 로깅하지 않고 그대로 통과시킨다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "request_id";

    private static final String H2_CONSOLE_PATH = "/h2-console";

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
                    new String(cachedRequest.getBody(), StandardCharsets.UTF_8));
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

    private boolean isH2ConsoleRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals(H2_CONSOLE_PATH) || path.startsWith(H2_CONSOLE_PATH + "/");
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
