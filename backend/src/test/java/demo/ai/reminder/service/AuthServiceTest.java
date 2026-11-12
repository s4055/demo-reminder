package demo.ai.reminder.service;

import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.user.RefreshToken;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.dto.request.LoginRequest;
import demo.ai.reminder.dto.request.SignupRequest;
import demo.ai.reminder.dto.response.UserResponse;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.exception.TooManyLoginAttemptsException;
import demo.ai.reminder.repository.user.RefreshTokenRepository;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.security.JwtProvider;
import demo.ai.reminder.support.TestAuth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {

    private static final String CLIENT_IP = "127.0.0.1";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    private AuthResult signupAlice() {
        return authService.signup(new SignupRequest("alice@example.com", "password1", "앨리스"));
    }

    private static void assertUnauthorized(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("가입하면 이메일은 소문자로, 비밀번호는 BCrypt 해시로 저장한다")
    void signup_normalizesEmail_andHashesPassword() {
        UserResponse response = authService.signup(new SignupRequest(" Alice@Example.COM ", "password1", "앨리스")).user();

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.name()).isEqualTo("앨리스");
        User user = userRepository.findById(response.id()).orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("password1").startsWith("$2");
        assertThat(passwordEncoder.matches("password1", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("가입하면 가입한 사용자의 액세스 토큰과 리프레시 토큰을 발급한다")
    void signup_issuesTokensForNewUser() {
        AuthResult tokens = signupAlice();

        assertThat(jwtProvider.verifyAccessToken(tokens.accessToken())).contains(tokens.user().id());
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(refreshTokenRepository.findAll()).singleElement()
                .satisfies(token -> assertThat(token.getUser().getId()).isEqualTo(tokens.user().id()));
    }

    @Test
    @DisplayName("이미 가입한 이메일로 가입하면 409 예외가 발생한다")
    void signup_throwsConflict_whenEmailExists() {
        signupAlice();

        assertThatThrownBy(() -> authService.signup(new SignupRequest("ALICE@example.com", "password2", "앨리스2")))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.CONFLICT);
    }

    @Test
    @DisplayName("이메일 컬럼은 최대 길이까지만 저장된다")
    void emailColumn_rejectsEmailsLongerThanMaxLength() {
        String domain = "@example.com";
        String maxEmail = "a".repeat(User.EMAIL_MAX_LENGTH - domain.length()) + domain;

        User saved = userRepository.saveAndFlush(new User(maxEmail, "password1", "앨리스"));

        assertThat(saved.getEmail()).isEqualTo(maxEmail);
        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("a" + maxEmail, "password1", "앨리스")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("이름 컬럼은 최대 길이까지만 저장된다")
    void nameColumn_rejectsNamesLongerThanMaxLength() {
        String maxName = "가".repeat(User.NAME_MAX_LENGTH);

        User saved = userRepository.saveAndFlush(new User("alice@example.com", "password1", maxName));

        assertThat(saved.getName()).isEqualTo(maxName);
        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("bob@example.com", "password1", maxName + "가")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("이메일/비밀번호가 맞으면 해당 사용자의 토큰과 사용자 정보를 반환한다")
    void login_withValidCredentials_issuesTokens() {
        Long aliceId = signupAlice().user().id();

        AuthResult tokens = authService.login(new LoginRequest("Alice@example.com", "password1"), CLIENT_IP);

        assertThat(tokens.user().id()).isEqualTo(aliceId);
        assertThat(tokens.user().email()).isEqualTo("alice@example.com");
        assertThat(jwtProvider.verifyAccessToken(tokens.accessToken())).contains(aliceId);
        assertThat(tokens.toString()).doesNotContain(tokens.accessToken(), tokens.refreshToken());
    }

    @Test
    @DisplayName("로그인하면 그 사용자의 만료된 리프레시 토큰을 삭제하고 유효한 토큰은 남긴다")
    void login_deletesExpiredRefreshTokensOfUser() {
        AuthResult signedUp = signupAlice();
        User alice = userRepository.findById(signedUp.user().id()).orElseThrow();
        User bob = userRepository.save(new User("bob@example.com", "{noop}password", "밥"));
        Instant past = Instant.now().minus(Duration.ofMinutes(1));
        RefreshToken aliceExpired = refreshTokenRepository.save(new RefreshToken(alice, "alice-expired", past));
        RefreshToken bobExpired = refreshTokenRepository.save(new RefreshToken(bob, "bob-expired", past));

        authService.login(new LoginRequest("alice@example.com", "password1"), CLIENT_IP);

        assertThat(refreshTokenRepository.findById(aliceExpired.getId())).isEmpty();
        assertThat(refreshTokenRepository.findById(bobExpired.getId())).isPresent();
        assertThat(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(alice.getId())).hasSize(2);
        assertThat(authService.refresh(signedUp.refreshToken()).user().id()).isEqualTo(alice.getId());
    }

    @Test
    @DisplayName("비밀번호가 틀리거나 없는 이메일이면 401 예외가 발생한다")
    void login_withInvalidCredentials_throwsUnauthorized() {
        signupAlice();

        assertUnauthorized(() -> authService.login(new LoginRequest("alice@example.com", "wrong-password"), CLIENT_IP));
        assertUnauthorized(() -> authService.login(new LoginRequest("nobody@example.com", "password1"), CLIENT_IP));
    }

    @Test
    @DisplayName("같은 이메일로 5번 틀리면 올바른 비밀번호로도 로그인을 막고 남은 차단 시간을 담은 429 예외가 발생한다")
    void login_afterFiveFailures_blocksEvenWithCorrectPassword() {
        authService.signup(new SignupRequest("locked@example.com", "password1", "잠김"));
        for (int i = 0; i < 5; i++) {
            assertUnauthorized(() -> authService.login(new LoginRequest("locked@example.com", "wrong"), "10.1.0.1"));
        }

        assertThatThrownBy(() -> authService.login(new LoginRequest("Locked@example.com", "password1"), "10.1.0.2"))
                .isInstanceOfSatisfying(TooManyLoginAttemptsException.class, e -> {
                    assertThat(e.getResultCode()).isEqualTo(ResultCode.TOO_MANY_LOGIN_ATTEMPTS);
                    assertThat(e.getRetryAfter()).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(15));
                });
    }

    @Test
    @DisplayName("다른 이메일의 로그인 실패는 이메일 기준 차단에 섞이지 않는다")
    void login_failuresOfOtherEmail_doNotBlock() {
        authService.signup(new SignupRequest("carol@example.com", "password1", "캐럴"));
        for (int i = 0; i < 4; i++) {
            assertUnauthorized(() -> authService.login(new LoginRequest("carol@example.com", "wrong"), "10.1.1.1"));
            assertUnauthorized(() -> authService.login(new LoginRequest("dave@example.com", "wrong"), "10.1.1.1"));
        }

        assertThat(authService.login(new LoginRequest("carol@example.com", "password1"), "10.1.1.1").user().email())
                .isEqualTo("carol@example.com");
    }

    @Test
    @DisplayName("로그인에 성공하면 그 이메일의 실패 횟수가 초기화된다")
    void login_success_resetsEmailFailures() {
        authService.signup(new SignupRequest("erin@example.com", "password1", "에린"));
        for (int i = 0; i < 4; i++) {
            assertUnauthorized(() -> authService.login(new LoginRequest("erin@example.com", "wrong"), "10.1.2.1"));
        }
        authService.login(new LoginRequest("erin@example.com", "password1"), "10.1.2.1");
        for (int i = 0; i < 4; i++) {
            assertUnauthorized(() -> authService.login(new LoginRequest("erin@example.com", "wrong"), "10.1.2.1"));
        }

        assertThat(authService.login(new LoginRequest("erin@example.com", "password1"), "10.1.2.1").user().email())
                .isEqualTo("erin@example.com");
    }

    @Test
    @DisplayName("리프레시 토큰으로 재발급하면 새 토큰과 사용자 정보를 반환하고 이전 리프레시 토큰은 쓸 수 없다")
    void refresh_rotatesRefreshToken() {
        AuthResult signedUp = signupAlice();

        AuthResult refreshed = authService.refresh(signedUp.refreshToken());

        assertThat(refreshed.user().email()).isEqualTo("alice@example.com");
        assertThat(jwtProvider.verifyAccessToken(refreshed.accessToken())).contains(signedUp.user().id());
        assertThat(refreshed.refreshToken()).isNotEqualTo(signedUp.refreshToken());
        assertUnauthorized(() -> authService.refresh(signedUp.refreshToken()));
    }

    @Test
    @DisplayName("리프레시 토큰이 없거나 모르는 값이면 재발급할 때 401 예외가 발생한다")
    void refresh_withMissingOrUnknownToken_throwsUnauthorized() {
        assertUnauthorized(() -> authService.refresh(null));
        assertUnauthorized(() -> authService.refresh("unknown-token"));
    }

    @Test
    @DisplayName("로그아웃하면 리프레시 토큰이 폐기되어 재발급할 수 없다")
    void logout_revokesRefreshToken() {
        AuthResult tokens = signupAlice();

        authService.logout(tokens.refreshToken());

        assertUnauthorized(() -> authService.refresh(tokens.refreshToken()));
    }

    @Test
    @DisplayName("리프레시 토큰 없이 로그아웃해도 예외가 발생하지 않는다")
    void logout_withoutToken_succeeds() {
        assertThatCode(() -> authService.logout(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("me는 현재 로그인한 사용자 정보를 반환한다")
    void me_returnsCurrentUser() {
        User alice = TestAuth.signIn(userRepository, "alice@example.com");

        UserResponse result = authService.me();

        assertThat(result.id()).isEqualTo(alice.getId());
        assertThat(result.email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("로그인하지 않은 상태에서 me를 호출하면 401 예외가 발생한다")
    void me_throwsUnauthorized_whenNotLoggedIn() {
        SecurityContextHolder.clearContext();

        assertUnauthorized(() -> authService.me());
    }
}
