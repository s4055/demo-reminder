package demo.ai.reminder.security;

import demo.ai.reminder.common.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 현재 요청의 로그인 사용자. 서비스는 이 값으로 조회/수정 대상을 자기 데이터로 제한한다.
 * /api/** 는 SecurityConfig에서 이미 로그인을 요구하므로, 로그인 정보가 없으면 401로 응답한다.
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;

    public Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Login required");
        }
        return loginUser.getId();
    }

    // 소유자 FK를 채울 때 쓰는 참조. 조회 쿼리 없이 id만으로 만든다.
    public User reference() {
        return userRepository.getReferenceById(id());
    }

    public User load() {
        Long id = id();
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "User not found: " + id));
    }
}
