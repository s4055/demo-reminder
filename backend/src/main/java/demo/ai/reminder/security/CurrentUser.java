package demo.ai.reminder.security;

import demo.ai.reminder.exception.BusinessException;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.User;
import demo.ai.reminder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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

    public User reference() {
        return userRepository.getReferenceById(id());
    }

    public User load() {
        Long id = id();
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "User not found: " + id));
    }
}
