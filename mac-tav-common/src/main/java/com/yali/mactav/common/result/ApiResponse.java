package com.yali.mactav.common.result;

import com.yali.mactav.common.enums.ErrorCode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API response envelope used by web-facing code.
 *
 * <p>The common module only defines the response shape and error metadata; it
 * does not own controller behavior, workflow orchestration, or exception
 * rendering policy.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;

    private Integer code;

    private String errorCode;

    private String message;

    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(ErrorCode.SUCCESS.getCode())
                .errorCode(ErrorCode.SUCCESS.getErrorCode())
                .message(ErrorCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .errorCode(errorCode.getErrorCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static <T> ApiResponse<T> fail(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(ErrorCode.INTERNAL_ERROR.getCode())
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
