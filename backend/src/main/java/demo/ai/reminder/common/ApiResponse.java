package demo.ai.reminder.common;

/**
 * 모든 API 응답의 공통 형태. 성공 시 data에 결과를, 실패 시 data는 null이다.
 */
public record ApiResponse<T>(
        String resultCode,
        String resultMsg,
        T data
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResultCode.SUCCESS.name(), ResultCode.SUCCESS.getDefaultMessage(), data);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> error(ResultCode resultCode, String resultMsg) {
        return new ApiResponse<>(resultCode.name(), resultMsg, null);
    }
}
