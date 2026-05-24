package com.yali.mactav.common.enums;

public enum ErrorCode {
    OK,
    BAD_REQUEST,
    TASK_NOT_FOUND,
    STAGE_NOT_READY,
    PIPELINE_FAILED,
    MODEL_CALL_FAILED,
    AGENT_SCHEMA_INVALID,
    AGENT_PARSE_FAILED,
    AGENT_OUTPUT_INVALID,
    TOOL_CALL_FAILED,
    MCP_CALL_FAILED,
    A2A_CALL_FAILED,
    INTERNAL_ERROR;

    public static ErrorCode from(String value, ErrorCode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return ErrorCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
