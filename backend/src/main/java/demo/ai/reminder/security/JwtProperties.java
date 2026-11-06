package demo.ai.reminder.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl, boolean cookieSecure) {

    public static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("app.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        Objects.requireNonNull(accessTokenTtl, "app.jwt.access-token-ttl is required");
        Objects.requireNonNull(refreshTokenTtl, "app.jwt.refresh-token-ttl is required");
    }

    public byte[] secretBytes() {
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "JwtProperties[accessTokenTtl=" + accessTokenTtl + ", refreshTokenTtl=" + refreshTokenTtl
                + ", cookieSecure=" + cookieSecure + "]";
    }
}
