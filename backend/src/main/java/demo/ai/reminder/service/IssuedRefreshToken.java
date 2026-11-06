package demo.ai.reminder.service;

public record IssuedRefreshToken(Long userId, String token) {

    @Override
    public String toString() {
        return "IssuedRefreshToken[userId=" + userId + "]";
    }
}
