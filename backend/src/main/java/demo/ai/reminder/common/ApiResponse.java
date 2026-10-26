package demo.ai.reminder.common;

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
