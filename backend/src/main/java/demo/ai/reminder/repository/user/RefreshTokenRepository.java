package demo.ai.reminder.repository.user;

import demo.ai.reminder.domain.user.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    long deleteByUserIdAndExpiresAtLessThanEqual(Long userId, Instant now);
}
