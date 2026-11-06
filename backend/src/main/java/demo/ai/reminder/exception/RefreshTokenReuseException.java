package demo.ai.reminder.exception;

import demo.ai.reminder.common.ResultCode;

public class RefreshTokenReuseException extends BusinessException {

    public RefreshTokenReuseException(Long userId) {
        super(ResultCode.UNAUTHORIZED, "Revoked refresh token was reused; all refresh tokens of user " + userId
                + " are revoked");
    }
}
