package demo.ai.reminder.controller;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.dto.request.LoginRequest;
import demo.ai.reminder.dto.request.SignupRequest;
import demo.ai.reminder.dto.response.UserResponse;
import demo.ai.reminder.security.LoginSession;
import demo.ai.reminder.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 세션(쿠키) 기반 회원가입/로그인/로그아웃. 세션의 시작과 종료는 LoginSession이 맡는다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginSession loginSession;

    // 가입하면 바로 로그인된 상태가 된다.
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest request,
                                            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        UserResponse user = authService.signup(request.toCommand());
        loginSession.start(authService.authenticate(user.email(), request.password()), httpRequest, httpResponse);
        return ApiResponse.success(user);
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authService.authenticate(request.email(), request.password());
        loginSession.start(authentication, httpRequest, httpResponse);
        return ApiResponse.success(authService.getUser(authentication));
    }

    // 로그인하지 않은 상태에서 호출해도 성공으로 응답한다.
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        loginSession.end(httpRequest, httpResponse);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(authService.me());
    }
}
