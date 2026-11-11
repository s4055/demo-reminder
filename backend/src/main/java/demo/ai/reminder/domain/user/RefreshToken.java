package demo.ai.reminder.domain.user;

import demo.ai.reminder.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    public static final int TOKEN_HASH_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = TOKEN_HASH_LENGTH)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    @Version
    @ColumnDefault("0")
    @Column(nullable = false)
    private Long version;

    public RefreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable(LocalDateTime now) {
        return !isRevoked() && !isExpired(now);
    }

    public RefreshToken rotate(String newTokenHash, LocalDateTime newExpiresAt, LocalDateTime now) {
        if (!isUsable(now)) {
            throw new IllegalStateException("Cannot rotate a revoked or expired refresh token: id=" + id);
        }
        revokedAt = now;
        return new RefreshToken(user, newTokenHash, newExpiresAt);
    }

    public void revoke(LocalDateTime now) {
        if (!isRevoked()) {
            revokedAt = now;
        }
    }
}
