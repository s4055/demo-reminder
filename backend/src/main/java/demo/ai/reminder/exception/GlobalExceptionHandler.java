package demo.ai.reminder.exception;

import demo.ai.reminder.common.ApiResponse;
import demo.ai.reminder.common.ResultCode;
import demo.ai.reminder.domain.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return respond(ex.getResultCode(), ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomain(DomainException ex) {
        return respond(ResultCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        log.info("Optimistic locking failure: {}", ex.getMessage());
        return respond(ResultCode.VERSION_CONFLICT, ResultCode.VERSION_CONFLICT.getDefaultMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return respond(ResultCode.INTERNAL_SERVER_ERROR, ResultCode.INTERNAL_SERVER_ERROR.getDefaultMessage());
    }

    private ResponseEntity<ApiResponse<Void>> respond(ResultCode resultCode, String message) {
        return ResponseEntity.status(resultCode.getStatus()).body(ApiResponse.error(resultCode, message));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResultCode resultCode = resolveResultCode(ex, statusCode);
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(ApiResponse.error(resultCode, resolveMessage(ex, resultCode)));
    }

    private ResultCode resolveResultCode(Exception ex, HttpStatusCode statusCode) {
        return switch (ex) {
            case HttpRequestMethodNotSupportedException e -> ResultCode.METHOD_NOT_ALLOWED;
            case NoHandlerFoundException e -> ResultCode.NOT_FOUND;
            case NoResourceFoundException e -> ResultCode.NOT_FOUND;
            case MissingPathVariableException e -> ResultCode.INTERNAL_SERVER_ERROR;
            case ServletRequestBindingException e -> ResultCode.BAD_REQUEST;
            case MissingServletRequestPartException e -> ResultCode.BAD_REQUEST;
            case MethodArgumentNotValidException e -> ResultCode.BAD_REQUEST;
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
        if (ex instanceof HandlerMethodValidationException validation && resultCode == ResultCode.BAD_REQUEST) {
            return validation.getParameterValidationResults().stream()
                    .flatMap(result -> result.getResolvableErrors().stream()
                            .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
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
