package demo.ai.reminder.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * 로그인 세션의 시작과 종료. 세션에 인증 정보를 저장하고, 응답의 JSESSIONID 쿠키로 이후 요청을 식별한다.
 */
@Component
@RequiredArgsConstructor
public class LoginSession {

    private final SecurityContextRepository securityContextRepository;
    private final LogoutHandler logoutHandler;

    // 세션 고정 공격을 막기 위해 기존 세션이 있으면 세션 ID를 바꾼 뒤 인증 정보를 저장한다.
    public void start(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    // 세션을 무효화하고 인증 정보를 지운다. 로그인하지 않은 상태에서 호출해도 된다.
    public void end(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }
}
