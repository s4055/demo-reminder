package demo.ai.reminder.dto.response;

import demo.ai.reminder.domain.user.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String name,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
    }
}
