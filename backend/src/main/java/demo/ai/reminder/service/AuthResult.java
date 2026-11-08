package demo.ai.reminder.service;

import demo.ai.reminder.dto.response.UserResponse;

public record AuthResult(String accessToken, String refreshToken, UserResponse user) {

    @Override
    public String toString() {
        return "AuthResult[user=" + user + "]";
    }
}
