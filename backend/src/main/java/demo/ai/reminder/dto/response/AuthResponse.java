package demo.ai.reminder.dto.response;

public record AuthResponse(String accessToken, UserResponse user) {

    @Override
    public String toString() {
        return "AuthResponse[user=" + user + "]";
    }
}
