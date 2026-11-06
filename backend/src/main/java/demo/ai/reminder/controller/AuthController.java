package demo.ai.reminder.controller;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.dto.request.LoginRequest;
import demo.ai.reminder.dto.request.SignupRequest;
import demo.ai.reminder.dto.response.AuthResponse;
import demo.ai.reminder.dto.response.UserResponse;
import demo.ai.reminder.security.RefreshTokenCookie;
import demo.ai.reminder.service.AuthService;
import demo.ai.reminder.service.AuthTokens;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody SignupRequest request, HttpServletResponse response) {
        return ApiResponse.success(respond(authService.signup(request), response));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return ApiResponse.success(respond(authService.login(request), response));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        return ApiResponse.success(respond(authService.refresh(refreshToken), response));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        refreshTokenCookie.clear(response);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(authService.me());
    }

    private AuthResponse respond(AuthTokens tokens, HttpServletResponse response) {
        refreshTokenCookie.write(response, tokens.refreshToken());
        return tokens.toResponse();
    }
}
