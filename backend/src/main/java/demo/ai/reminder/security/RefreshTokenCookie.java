package demo.ai.reminder.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";
    public static final String PATH = "/api/auth";

    private final JwtProperties jwtProperties;

    public void write(HttpServletResponse response, String refreshToken) {
        add(response, build(refreshToken, jwtProperties.refreshTokenTtl()));
    }

    public void clear(HttpServletResponse response) {
        add(response, build("", Duration.ZERO));
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(jwtProperties.cookieSecure())
                .sameSite("Lax")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }

    private static void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
