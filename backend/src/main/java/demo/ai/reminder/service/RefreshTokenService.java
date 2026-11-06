package demo.ai.reminder.service;

import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.user.RefreshToken;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.exception.RefreshTokenReuseException;
import demo.ai.reminder.repository.user.RefreshTokenRepository;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public IssuedRefreshToken issue(Long userId) {
        User user = userRepository.getReferenceById(userId);
        String token = generateToken();
        refreshTokenRepository.save(new RefreshToken(user, hash(token), expiresAt(now())));
        return new IssuedRefreshToken(userId, token);
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public IssuedRefreshToken rotate(String token) {
        LocalDateTime now = now();
        RefreshToken current = find(token);
        if (current.isRevoked()) {
            Long userId = current.getUser().getId();
            revokeAll(userId, now);
            throw new RefreshTokenReuseException(userId);
        }
        if (current.isExpired(now)) {
            throw invalidToken("Refresh token is expired");
        }
        String newToken = generateToken();
        refreshTokenRepository.save(current.rotate(hash(newToken), expiresAt(now), now));
        flushRotation();
        return new IssuedRefreshToken(current.getUser().getId(), newToken);
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(token))
                .ifPresent(refreshToken -> refreshToken.revoke(now()));
    }

    public long deleteExpired(Long userId) {
        return refreshTokenRepository.deleteByUserIdAndExpiresAtLessThanEqual(userId, now());
    }

    private RefreshToken find(String token) {
        if (token == null || token.isBlank()) {
            throw invalidToken("Refresh token is missing");
        }
        return refreshTokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> invalidToken("Refresh token is unknown"));
    }

    private void revokeAll(Long userId, LocalDateTime now) {
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(refreshToken -> refreshToken.revoke(now));
    }

    private void flushRotation() {
        try {
            refreshTokenRepository.flush();
        } catch (OptimisticLockingFailureException e) {
            throw invalidToken("Refresh token was rotated concurrently");
        }
    }

    private static BusinessException invalidToken(String message) {
        return new BusinessException(ResultCode.UNAUTHORIZED, message);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private LocalDateTime expiresAt(LocalDateTime now) {
        return now.plus(jwtProperties.refreshTokenTtl());
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
