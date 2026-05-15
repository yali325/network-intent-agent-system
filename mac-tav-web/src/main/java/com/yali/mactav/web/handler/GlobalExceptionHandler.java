package com.yali.mactav.web.handler;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        return ResponseEntity
                .status(statusFor(ex.getErrorCode()))
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            MissingPathVariableException.class,
            MissingServletRequestParameterException.class,
            HttpRequestMethodNotSupportedException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.BAD_REQUEST, badRequestMessage(ex)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.TASK_NOT_FOUND, "Resource not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnknown(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR, "Internal server error"));
    }

    private HttpStatus statusFor(ErrorCode errorCode) {
        if (errorCode == ErrorCode.BAD_REQUEST) {
            return HttpStatus.BAD_REQUEST;
        }
        if (errorCode == ErrorCode.TASK_NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        if (errorCode == ErrorCode.STAGE_NOT_READY) {
            return HttpStatus.CONFLICT;
        }
        if (errorCode == ErrorCode.PIPELINE_FAILED || errorCode == ErrorCode.INTERNAL_ERROR) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String badRequestMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException validationException
                && validationException.getBindingResult().hasFieldErrors()) {
            return validationException.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "Bad request";
        }
        return ex instanceof HttpMessageNotReadableException ? "Bad request" : ex.getMessage();
    }
}
