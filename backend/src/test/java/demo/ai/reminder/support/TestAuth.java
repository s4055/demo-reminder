package demo.ai.reminder.support;

import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.security.AuthenticatedUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;

import java.util.List;

public final class TestAuth {

    private TestAuth() {
    }

    public static User signIn(UserRepository userRepository, String email) {
        User user = userRepository.save(new User(email, "{noop}password", "테스터"));
        signIn(user);
        return user;
    }

    public static void signIn(User user) {
        TestSecurityContextHolder.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(user.getId()), null, List.of()));
    }
}
