package demo.ai.reminder.service;

import demo.ai.reminder.dto.request.LoginRequest;
import demo.ai.reminder.dto.request.SignupRequest;
import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.exception.RefreshTokenReuseException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.user.User;
import demo.ai.reminder.dto.response.UserResponse;
import demo.ai.reminder.repository.user.UserRepository;
import demo.ai.reminder.security.CurrentUser;
import demo.ai.reminder.security.JwtProvider;
import demo.ai.reminder.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResult signup(SignupRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ResultCode.CONFLICT, "Email already registered: " + email);
        }
        User user = userRepository.save(
                new User(email, passwordEncoder.encode(request.password()), request.strippedName()));
        return issueTokens(user, refreshTokenService.issue(user.getId()));
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        Long userId = authenticate(request.email(), request.password());
        refreshTokenService.deleteExpired(userId);
        return issueTokens(loadUser(userId), refreshTokenService.issue(userId));
    }

    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public AuthResult refresh(String refreshToken) {
        IssuedRefreshToken rotated = refreshTokenService.rotate(refreshToken);
        return issueTokens(loadUser(rotated.userId()), rotated);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    public UserResponse me() {
        return UserResponse.from(currentUser.load());
    }

    private Long authenticate(String email, String password) {
        try {
            LoginUser loginUser = (LoginUser) authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, password)).getPrincipal();
            return loginUser.getId();
        } catch (AuthenticationException e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Invalid email or password");
        }
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "User not found: " + userId));
    }

    private AuthResult issueTokens(User user, IssuedRefreshToken refreshToken) {
        return new AuthResult(jwtProvider.issueAccessToken(user.getId()), refreshToken.token(),
                UserResponse.from(user));
    }
}
