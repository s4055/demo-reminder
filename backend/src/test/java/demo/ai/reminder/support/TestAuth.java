package demo.ai.reminder.support;

import demo.ai.reminder.domain.User;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.security.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;

/**
 * 테스트에서 로그인한 사용자를 만든다.
 * TestSecurityContextHolder에 넣은 인증은 서비스(SecurityContextHolder)와 MockMvc 요청 모두에 적용되고,
 * 각 테스트가 끝나면 spring-security-test가 지운다.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static User signIn(UserRepository userRepository, String email) {
        User user = userRepository.save(new User(email, "{noop}password", "테스터"));
        signIn(user);
        return user;
    }

    public static void signIn(User user) {
        LoginUser loginUser = new LoginUser(user);
        TestSecurityContextHolder.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(loginUser, null, loginUser.getAuthorities()));
    }
}
