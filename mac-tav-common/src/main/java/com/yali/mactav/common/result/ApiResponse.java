package com.yali.mactav.common.result;

import com.yali.mactav.common.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(ErrorCode.OK.name())
                .message("success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.name());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.name())
                .message(message)
                .build();
    }
}
