package com.yali.mactav.web.handler;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.ApiResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts project exceptions into the shared ApiResponse envelope.
 *
 * <p>The handler keeps stack traces and sensitive runtime details out of Web
 * responses while preserving stable error codes for API clients.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = resolveErrorCode(exception.getErrorCode());
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .code(errorCode.getCode())
                .errorCode(exception.getErrorCode())
                .message(exception.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(resolveHttpStatus(errorCode)).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException exception) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .code(ErrorCode.INTERNAL_ERROR.getCode())
                .errorCode(ErrorCode.INTERNAL_ERROR.getErrorCode())
                .message(ErrorCode.INTERNAL_ERROR.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ErrorCode resolveErrorCode(String errorCode) {
        return Arrays.stream(ErrorCode.values())
                .filter(candidate -> candidate.getErrorCode().equals(errorCode))
                .findFirst()
                .orElse(ErrorCode.INTERNAL_ERROR);
    }

    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        if (errorCode == ErrorCode.BAD_REQUEST || errorCode == ErrorCode.PARAM_INVALID) {
            return HttpStatus.BAD_REQUEST;
        }
        if (errorCode == ErrorCode.WORKSPACE_NOT_FOUND
                || errorCode == ErrorCode.ARTIFACT_NOT_FOUND
                || errorCode == ErrorCode.REPAIR_PLAN_NOT_FOUND
                || errorCode == ErrorCode.REPAIR_ACTION_NOT_FOUND
                || errorCode == ErrorCode.AGENT_CARD_NOT_FOUND
                || errorCode == ErrorCode.RESOURCE_NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        if (errorCode == ErrorCode.WORKSPACE_STATE_INVALID
                || errorCode == ErrorCode.STAGE_NOT_READY
                || errorCode == ErrorCode.REPAIR_ACTION_NOT_APPROVED) {
            return HttpStatus.CONFLICT;
        }
        if (errorCode == ErrorCode.AGENT_SERVICE_UNAVAILABLE) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (errorCode == ErrorCode.REMOTE_AGENT_TIMEOUT) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (errorCode == ErrorCode.A2A_CALL_FAILED || errorCode == ErrorCode.AGENT_DISCOVERY_FAILED) {
            return HttpStatus.BAD_GATEWAY;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
