package demo.ai.reminder.service;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.SignupRequest;
import demo.ai.reminder.dto.UserResponse;
import demo.ai.reminder.repository.UserRepository;
import demo.ai.reminder.security.CurrentUser;
import demo.ai.reminder.security.LoginUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    // BCrypt가 비교에 쓰는 최대 길이. 넘으면 뒷부분이 무시되므로 가입 단계에서 막는다.
    private static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CurrentUser currentUser;

    // 이메일은 소문자로 맞춰 저장하므로 대소문자만 다른 이메일로는 중복 가입할 수 없다.
    @Transactional
    public UserResponse signup(SignupRequest request) {
        String email = LoginUserDetailsService.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ResultCode.CONFLICT, "Email already registered: " + email);
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "password must be at most 72 bytes");
        }
        return UserResponse.from(
                userRepository.save(new User(email, passwordEncoder.encode(request.password()), request.name().trim())));
    }

    // 현재 로그인한 사용자 정보.
    public UserResponse me() {
        return UserResponse.from(currentUser.load());
    }

    /**
     * 이메일/비밀번호를 확인해 인증 정보를 돌려준다. 세션 저장은 호출하는 쪽(AuthController)이 한다.
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
