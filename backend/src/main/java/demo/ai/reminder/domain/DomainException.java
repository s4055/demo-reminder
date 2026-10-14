package demo.ai.reminder.domain;

/**
 * 도메인 규칙 위반. 요청 값이 규칙에 어긋나 생기므로 GlobalExceptionHandler가 400(BAD_REQUEST)으로 응답한다.
 * 도메인은 HTTP 응답을 알지 않도록 ResultCode를 담지 않는다.
 * 일어나면 안 되는 호출(프로그래밍 오류)은 이 예외가 아니라 IllegalStateException 등 표준 예외로 던져 500으로 드러낸다.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
