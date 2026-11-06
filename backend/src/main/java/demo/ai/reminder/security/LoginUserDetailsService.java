package demo.ai.reminder.security;

import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) {
        return userRepository.findByEmail(User.normalizeEmail(email))
                .map(LoginUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
