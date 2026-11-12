package demo.ai.reminder.exception;

import demo.ai.reminder.common.ResultCode;
import lombok.Getter;

import java.time.Duration;

@Getter
public class TooManyLoginAttemptsException extends BusinessException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super(ResultCode.TOO_MANY_LOGIN_ATTEMPTS);
        this.retryAfter = retryAfter;
    }

    public long retryAfterSeconds() {
        long seconds = retryAfter.toSeconds();
        return retryAfter.toNanosPart() == 0 ? Math.max(seconds, 1) : seconds + 1;
    }
}
