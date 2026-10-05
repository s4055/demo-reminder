package demo.ai.reminder.dto;

import demo.ai.reminder.domain.User;

import java.time.LocalDateTime;

// 비밀번호 해시는 응답에 담지 않는다.
public record UserResponse(
        Long id,
        String email,
        String name,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}
