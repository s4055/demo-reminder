package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.User;

import java.time.LocalDateTime;

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
