package demo.ai.reminder.service;

import demo.ai.reminder.dto.response.AuthResponse;
import demo.ai.reminder.dto.response.UserResponse;

public record AuthTokens(String accessToken, String refreshToken, UserResponse user) {

    public AuthResponse toResponse() {
        return new AuthResponse(accessToken, user);
    }

    @Override
    public String toString() {
        return "AuthTokens[user=" + user + "]";
    }
}
