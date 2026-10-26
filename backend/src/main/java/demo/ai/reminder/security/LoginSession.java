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

    public void end(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
    }
}
