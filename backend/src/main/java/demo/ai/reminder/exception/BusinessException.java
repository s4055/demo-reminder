package demo.ai.reminder.exception;

import demo.ai.reminder.common.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode) {
        this(resultCode, resultCode.getDefaultMessage());
    }
}
