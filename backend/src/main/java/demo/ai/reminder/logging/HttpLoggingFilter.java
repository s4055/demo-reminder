package demo.ai.reminder.logging;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID = "request_id";

    private static final Pattern PASSWORD_FIELD = Pattern.compile("(\"password\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"");

    private static final String MASK = "****";

    private static final Set<String> MASKED_HEADERS = Set.of("cookie", "authorization");

    private static final int SESSION_FINGERPRINT_LENGTH = 12;

    private final List<String> excludedPaths;

    public HttpLoggingFilter(HttpLoggingProperties properties) {
        this.excludedPaths = properties.excludedPaths();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return excludedPaths.stream().anyMatch(excluded -> isUnder(path, excluded));
    }

    private static boolean isUnder(String path, String basePath) {
        return path.equals(basePath) || path.startsWith(basePath + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
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
            MDC.remove(REQUEST_ID);
        }
    }

    static String maskPassword(String body) {
        return PASSWORD_FIELD.matcher(body).replaceAll("$1\"" + MASK + "\"");
    }

    private String requestHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .map(name -> name + "=" + headerValue(request, name))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String headerValue(HttpServletRequest request, String name) {
        if (MASKED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
            return MASK;
        }
        return String.join(", ", Collections.list(request.getHeaders(name)));
    }

    private String sessionInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "none";
        }
        String attributeNames = String.join(", ", Collections.list(session.getAttributeNames()));
        return "[id=" + sessionFingerprint(session.getId()) + ", attributes=[" + attributeNames + "]]";
    }

    static String sessionFingerprint(String sessionId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, SESSION_FINGERPRINT_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
