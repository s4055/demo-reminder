package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.SignupRequest;
import demo.ai.reminder.security.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("가입하면 이메일은 소문자로, 비밀번호는 BCrypt 해시로 저장한다")
    void signup_normalizesEmail_andHashesPassword() {
        User user = authService.signup(new SignupRequest(" Alice@Example.COM ", "password1", " 앨리스 "));

        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getName()).isEqualTo("앨리스");
        assertThat(user.getPassword()).isNotEqualTo("password1").startsWith("$2");
        assertThat(passwordEncoder.matches("password1", user.getPassword())).isTrue();
    }

    @Test
    @DisplayName("이미 가입한 이메일로 가입하면 409 예외가 발생한다")
    void signup_throwsConflict_whenEmailExists() {
        authService.signup(new SignupRequest("alice@example.com", "password1", "앨리스"));

        assertThatThrownBy(() -> authService.signup(new SignupRequest("ALICE@example.com", "password2", "앨리스2")))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.CONFLICT);
    }

    @Test
    @DisplayName("비밀번호가 72바이트를 넘으면 400 예외가 발생한다 (BCrypt 제한)")
    void signup_throwsBadRequest_whenPasswordExceeds72Bytes() {
        String password = "가".repeat(25); // 25자 = 75바이트

        assertThatThrownBy(() -> authService.signup(new SignupRequest("alice@example.com", password, "앨리스")))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("이메일/비밀번호가 맞으면 비밀번호가 지워진 로그인 사용자로 인증된다")
    void authenticate_withValidCredentials_returnsLoginUserWithoutPassword() {
        User user = authService.signup(new SignupRequest("alice@example.com", "password1", "앨리스"));

        Authentication authentication = authService.authenticate("Alice@example.com", "password1");

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        assertThat(loginUser.getId()).isEqualTo(user.getId());
        assertThat(loginUser.getPassword()).isNull();
        assertThat(loginUser.toString()).doesNotContain(user.getPassword());
    }

    @Test
    @DisplayName("비밀번호가 틀리거나 없는 이메일이면 401 예외가 발생한다")
    void authenticate_withInvalidCredentials_throwsUnauthorized() {
        authService.signup(new SignupRequest("alice@example.com", "password1", "앨리스"));

        assertThatThrownBy(() -> authService.authenticate("alice@example.com", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.UNAUTHORIZED);
        assertThatThrownBy(() -> authService.authenticate("nobody@example.com", "password1"))
                .isInstanceOf(BusinessException.class)
                .extracting("resultCode").isEqualTo(ResultCode.UNAUTHORIZED);
    }
}
