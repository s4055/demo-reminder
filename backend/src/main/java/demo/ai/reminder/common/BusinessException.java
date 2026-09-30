package demo.ai.reminder.common;

import lombok.Getter;

/**
 * 도메인/비즈니스 규칙 위반을 나타내는 예외. GlobalExceptionHandler가 resultCode의 HTTP 상태와 메시지로 응답한다.
 */
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
