package demo.ai.reminder.exception;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
 *   <li>서비스 계층은 {@link BusinessException}을 던지고, {@link #handleBusiness}가
 *       예외에 담긴 ResultCode의 상태 코드와 예외 메시지로 응답한다.</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ResultCode resultCode = ex.getResultCode();
        return ResponseEntity.status(resultCode.getStatus())
                .body(ApiResponse.error(resultCode, ex.getMessage()));
    }

    // 버전 검증을 통과한 뒤 같은 순간에 다른 요청이 먼저 저장하면 flush 시점에 낙관적 락 실패가 난다. 버전 불일치와 같은 409로 응답한다.
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        log.info("Optimistic locking failure: {}", ex.getMessage());
        ResultCode resultCode = ResultCode.VERSION_CONFLICT;
        return ResponseEntity.status(resultCode.getStatus())
                .body(ApiResponse.error(resultCode, resultCode.getDefaultMessage()));
    }

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
        ResultCode resultCode = resolveResultCode(ex, statusCode);
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(ApiResponse.error(resultCode, resolveMessage(ex, resultCode)));
    }

    /**
     * 상위 클래스가 처리하는 표준 예외마다 ResultCode를 고정한다. HTTP 상태 코드는 Spring이 정한 값을 그대로 쓴다
     * (예: 415 HttpMediaTypeNotSupportedException도 결과 코드는 BAD_REQUEST).
     * <p>
     * 상태 코드가 생성 시점에 정해져 클래스로 고정할 수 없는 예외(ResponseStatusException 등 ErrorResponseException,
     * 파라미터면 400·반환값이면 500인 HandlerMethodValidationException)와 이후 Spring에 추가될 예외는
     * 상태 코드의 대분류로만 정한다. 특정 결과 코드를 상태 코드로 역조회하지 않는다.
     * <p>
     * 하위 타입이 상위 타입보다 먼저 와야 한다 (MissingPathVariableException은 ServletRequestBindingException,
     * ConversionNotSupportedException은 TypeMismatchException의 하위 타입). 순서가 틀리면 컴파일 오류가 난다.
     */
    private ResultCode resolveResultCode(Exception ex, HttpStatusCode statusCode) {
        return switch (ex) {
            case HttpRequestMethodNotSupportedException e -> ResultCode.METHOD_NOT_ALLOWED;
            case NoHandlerFoundException e -> ResultCode.NOT_FOUND;
            case NoResourceFoundException e -> ResultCode.NOT_FOUND;
            // 경로 변수 누락은 요청이 아니라 매핑 선언의 오류라 Spring도 500으로 응답한다.
            case MissingPathVariableException e -> ResultCode.INTERNAL_SERVER_ERROR;
            case ServletRequestBindingException e -> ResultCode.BAD_REQUEST;
            case MissingServletRequestPartException e -> ResultCode.BAD_REQUEST;
            case MethodArgumentNotValidException e -> ResultCode.BAD_REQUEST;
            // 값을 변환할 수단이 없는 서버 쪽 문제라 500이다.
            case ConversionNotSupportedException e -> ResultCode.INTERNAL_SERVER_ERROR;
            case TypeMismatchException e -> ResultCode.BAD_REQUEST;
            case HttpMessageNotReadableException e -> ResultCode.BAD_REQUEST;
            case HttpMediaTypeNotSupportedException e -> ResultCode.BAD_REQUEST;
            case HttpMediaTypeNotAcceptableException e -> ResultCode.BAD_REQUEST;
            case MaxUploadSizeExceededException e -> ResultCode.BAD_REQUEST;
            case HttpMessageNotWritableException e -> ResultCode.INTERNAL_SERVER_ERROR;
            case MethodValidationException e -> ResultCode.INTERNAL_SERVER_ERROR;
            case AsyncRequestTimeoutException e -> ResultCode.INTERNAL_SERVER_ERROR;
            default -> statusCode.is4xxClientError() ? ResultCode.BAD_REQUEST : ResultCode.INTERNAL_SERVER_ERROR;
        };
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
