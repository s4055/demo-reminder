package demo.ai.reminder.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 응답의 resultCode. 응답 본문에는 enum 이름이 그대로 내려간다.
 * <p>
 * 결과 코드는 예외를 던지거나 처리하는 쪽이 정해서 쓴다. HTTP 상태로부터 결과 코드를 역으로 찾지 않는다
 * (같은 상태를 쓰는 코드가 여럿일 수 있다. 예: CONFLICT, VERSION_CONFLICT).
 */
@Getter
@RequiredArgsConstructor
public enum ResultCode {
    SUCCESS(HttpStatus.OK, "성공"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "이미 존재하는 대상입니다."),
    // 편집을 시작한 뒤 다른 사용자가 먼저 수정함 (요청 버전 불일치 또는 동시 저장 경합). 중복을 뜻하는 CONFLICT와 구분한다.
    VERSION_CONFLICT(HttpStatus.CONFLICT, "다른 사용자가 먼저 수정했습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
