package demo.ai.reminder.support;

import demo.ai.reminder.domain.User;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.security.LoginUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;

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
