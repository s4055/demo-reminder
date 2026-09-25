package demo.ai.reminder.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * 모든 예외를 ApiResponse 형태(resultCode, resultMsg)로 변환한다.
 * <p>
 * 표준 예외의 상태 코드 판단은 상속받은 {@link ResponseEntityExceptionHandler}가 하고,
 * 응답 형식은 {@link #handleExceptionInternal} 재정의로 ApiResponse로 통일한다.
 * <ul>
 *   <li>상위 클래스가 처리하는 예외(Spring MVC 표준 예외, ResponseStatusException 등 ErrorResponseException 하위)는
 *       모두 handleExceptionInternal을 거친다.</li>
 *   <li>그 예외들을 개별로 다르게 처리하려면 {@code @ExceptionHandler}가 아니라 해당 {@code handleXxx()}를 오버라이드한다.
 *       같은 예외에 {@code @ExceptionHandler}를 선언하면 매핑이 모호해져 기동에 실패한다.</li>
 *   <li>상위 클래스 목록에 없는 예외는 기존처럼 {@code @ExceptionHandler}로 처리하며,
 *       별도 핸들러가 없으면 {@link #handleUnexpected}에서 500으로 응답한다.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ResultCode resultCode = ResultCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(resultCode, resultCode.getDefaultMessage()));
    }

    /**
     * 상위 클래스가 처리하는 모든 예외의 응답이 최종적으로 거치는 메서드.
     * 기본 구현은 ProblemDetail 본문을 만들지만, 여기서는 상태 코드와 헤더는 그대로 두고 본문만 ApiResponse로 바꾼다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResultCode resultCode = ResultCode.fromStatus(statusCode);
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(ApiResponse.error(resultCode, resolveMessage(ex, resultCode)));
    }

    private String resolveMessage(Exception ex, ResultCode resultCode) {
        if (ex instanceof MethodArgumentNotValidException validation) {
            return validation.getBindingResult().getFieldErrors().stream()
                    .map(this::formatFieldError)
                    .collect(Collectors.joining(", "));
        }
        if (ex instanceof ErrorResponse errorResponse && errorResponse.getBody().getDetail() != null) {
            return errorResponse.getBody().getDetail();
        }
        return resultCode.getDefaultMessage();
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
