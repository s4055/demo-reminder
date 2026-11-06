package demo.ai.reminder.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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

    private static final Pattern SECRET_FIELD =
            Pattern.compile("(\"(?:password|accessToken)\"\\s*:\\s*)\"(?:[^\"\\\\]|\\\\.)*\"");

    private static final String MASK = "****";

    private static final Set<String> MASKED_HEADERS = Set.of("cookie", "authorization");

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
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        try {
            CachedBodyRequestWrapper cachedRequest = new CachedBodyRequestWrapper(request);
            log.info("[Request] {} {} body={}", request.getMethod(), request.getRequestURI(),
                    maskSecrets(new String(cachedRequest.getBody(), StandardCharsets.UTF_8)));
            log.info("[Header] {}", requestHeaders(request));

            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            try {
                filterChain.doFilter(cachedRequest, wrappedResponse);
            } finally {
                log.info("[Response] status={} body={}", wrappedResponse.getStatus(),
                        maskSecrets(new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8)));
                wrappedResponse.copyBodyToResponse();
            }
        } finally {
            MDC.remove(REQUEST_ID);
        }
    }

    static String maskSecrets(String body) {
        return SECRET_FIELD.matcher(body).replaceAll("$1\"" + MASK + "\"");
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
}
