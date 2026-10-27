package demo.ai.reminder.service;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.response.UserResponse;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.security.CurrentUser;
import demo.ai.reminder.service.command.SignupCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CurrentUser currentUser;

    @Transactional
    public UserResponse signup(SignupCommand command) {
        String email = User.normalizeEmail(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ResultCode.CONFLICT, "Email already registered: " + email);
        }
        return UserResponse.from(
                userRepository.save(new User(email, passwordEncoder.encode(command.password()), command.name())));
    }

    public UserResponse me() {
        return UserResponse.from(currentUser.load());
    }

    /**
     * 이메일/비밀번호를 확인해 인증 정보를 돌려준다. 세션 저장은 호출하는 쪽(AuthController → LoginSession)이 한다.
     * 어느 쪽이 틀렸는지 알려주지 않도록 실패 사유는 하나의 메시지로 응답한다.
     */
    public Authentication authenticate(String email, String password) {
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (AuthenticationException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Invalid email or password");
        }
    }
}
