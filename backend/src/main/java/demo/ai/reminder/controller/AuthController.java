package demo.ai.reminder.controller;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.dto.LoginRequest;
import demo.ai.reminder.dto.SignupRequest;
import demo.ai.reminder.dto.UserResponse;
import demo.ai.reminder.security.CurrentUser;
import demo.ai.reminder.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 세션(쿠키) 기반 회원가입/로그인/로그아웃.
 * 로그인에 성공하면 인증 정보를 세션에 저장하고, 응답의 JSESSIONID 쿠키로 이후 요청을 식별한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;
    private final SecurityContextRepository securityContextRepository;

    // 가입하면 바로 로그인된 상태가 된다.
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest request,
                                            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = authService.signup(request);
        startSession(authService.authenticate(user.getEmail(), request.password()), httpRequest, httpResponse);
        return ApiResponse.success(UserResponse.from(user));
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        startSession(authService.authenticate(request.email(), request.password()), httpRequest, httpResponse);
        return ApiResponse.success(UserResponse.from(currentUser.load()));
    }

    // 로그인하지 않은 상태에서 호출해도 성공으로 응답한다.
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse,
                SecurityContextHolder.getContext().getAuthentication());
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(UserResponse.from(currentUser.load()));
    }

    // 세션 고정 공격을 막기 위해 기존 세션이 있으면 세션 ID를 바꾼 뒤 인증 정보를 저장한다.
    private void startSession(Authentication authentication,
                              HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
