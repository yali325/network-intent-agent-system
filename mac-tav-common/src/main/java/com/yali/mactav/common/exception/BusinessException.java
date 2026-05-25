package com.yali.mactav.common.exception;

import com.yali.mactav.common.enums.ErrorCode;
import lombok.Getter;

/**
 * Project-level runtime exception carrying a stable {@link ErrorCode#errorCode} value.
 *
 * <p>This type is intentionally lightweight and lives in mac-tav-common so that
 * model-core, agent-core, orchestrator, and web can convert failures without
 * depending on each other's implementation classes.</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode.getErrorCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode.getErrorCode();
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode.getErrorCode();
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
