package demo.ai.reminder.service;

import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.dto.request.SignupRequest;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.exception.RefreshTokenReuseException;
import demo.ai.reminder.repository.user.RefreshTokenRepository;
import demo.ai.reminder.repository.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RefreshTokenRotationCommitTest {

    private static final String EMAIL = "rotation-commit@example.com";

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(EMAIL).ifPresent(user -> {
            refreshTokenRepository.deleteAll(refreshTokenRepository.findAll().stream()
                    .filter(token -> token.getUser().getId().equals(user.getId()))
                    .toList());
            userRepository.delete(user);
        });
    }

    private AuthResult signup() {
        return authService.signup(new SignupRequest(EMAIL, "password1", "회전"));
    }

    @Test
    @DisplayName("교체 전 토큰을 재사용하면 401을 반환하면서도 최신 토큰 폐기는 커밋되어 다시 로그인해야 한다")
    void refresh_withReusedToken_commitsRevocationOfLatestToken() {
        AuthResult signedUp = signup();
        AuthResult refreshed = authService.refresh(signedUp.refreshToken());

        assertThatThrownBy(() -> authService.refresh(signedUp.refreshToken()))
                .isInstanceOf(RefreshTokenReuseException.class)
                .extracting("resultCode").isEqualTo(ResultCode.UNAUTHORIZED);

        assertThat(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(signedUp.user().id())).isEmpty();
        assertThatThrownBy(() -> authService.refresh(refreshed.refreshToken()))
                .isInstanceOf(RefreshTokenReuseException.class);
    }

    @Test
    @DisplayName("같은 토큰을 동시에 회전해 버전이 충돌하면 늦은 쪽만 401이 되고 먼저 회전한 토큰은 유지된다")
    void refresh_concurrentRotation_rejectsLaterOneWithUnauthorized() {
        AuthResult signedUp = signup();
        TransactionTemplate later = new TransactionTemplate(transactionManager);
        TransactionTemplate earlier = new TransactionTemplate(transactionManager);
        earlier.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        AtomicReference<AuthResult> winner = new AtomicReference<>();

        assertThatThrownBy(() -> later.executeWithoutResult(status -> {
            refreshTokenRepository.findAll();
            earlier.executeWithoutResult(innerStatus -> winner.set(authService.refresh(signedUp.refreshToken())));
            authService.refresh(signedUp.refreshToken());
        }))
                .isInstanceOf(BusinessException.class)
                .isNotInstanceOf(RefreshTokenReuseException.class)
                .extracting("resultCode").isEqualTo(ResultCode.UNAUTHORIZED);

        assertThat(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(signedUp.user().id())).hasSize(1);
        assertThat(authService.refresh(winner.get().refreshToken()).user().email()).isEqualTo(EMAIL);
    }
}
