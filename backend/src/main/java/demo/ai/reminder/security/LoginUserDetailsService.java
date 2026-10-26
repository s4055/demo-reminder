package demo.ai.reminder.security;

import demo.ai.reminder.domain.User;
import demo.ai.reminder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(User.normalizeEmail(email))
                .map(LoginUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
