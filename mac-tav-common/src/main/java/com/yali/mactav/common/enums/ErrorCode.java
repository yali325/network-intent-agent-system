package com.yali.mactav.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Shared MAC-TAV error catalog for API responses and cross-module exceptions.
 *
 * <p>This enum belongs to mac-tav-common and must stay independent from web,
 * orchestrator, agent, persistence, and model-provider implementations.</p>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    OK(0, "OK", "OK"),
    SUCCESS(0, "OK", "Success"),
    BAD_REQUEST(400, "BAD_REQUEST", "Bad request"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "Internal error"),

    BUSINESS_ERROR(1000, "MAC_TAV_BUSINESS_ERROR", "Business error"),
    PARAM_INVALID(1001, "MAC_TAV_PARAM_INVALID", "Invalid request parameter"),
    RESOURCE_NOT_FOUND(1002, "MAC_TAV_RESOURCE_NOT_FOUND", "Resource not found"),
    SYSTEM_ERROR(1003, "MAC_TAV_SYSTEM_ERROR", "System error"),

    PROMPT_NOT_FOUND(1100, "PROMPT_NOT_FOUND", "Prompt resource not found"),
    PROMPT_LOAD_FAILED(1101, "PROMPT_LOAD_FAILED", "Prompt resource load failed"),

    MODEL_CALL_FAILED(2000, "MODEL_CALL_FAILED", "Model call failed"),
    AGENT_EXECUTION_FAILED(2001, "AGENT_EXECUTION_FAILED", "Agent execution failed"),
    AGENT_SCHEMA_INVALID(2002, "AGENT_SCHEMA_INVALID", "Agent response schema is invalid"),
    AGENT_PARSE_FAILED(2003, "AGENT_PARSE_FAILED", "Agent response parse failed"),
    AGENT_OUTPUT_INVALID(2004, "AGENT_OUTPUT_INVALID", "Agent output is invalid"),
    TOOL_CALL_FAILED(2005, "TOOL_CALL_FAILED", "Tool call failed"),
    MCP_CALL_FAILED(2006, "MCP_CALL_FAILED", "MCP call failed"),
    A2A_CALL_FAILED(50031, "A2A_CALL_FAILED", "A2A call failed"),
    A2A_RESPONSE_INVALID(50032, "A2A_RESPONSE_INVALID", "A2A response is invalid"),
    AGENT_DISCOVERY_FAILED(50033, "AGENT_DISCOVERY_FAILED", "Agent discovery failed"),
    REMOTE_AGENT_TIMEOUT(50431, "REMOTE_AGENT_TIMEOUT", "Remote agent call timed out"),
    AGENT_CARD_NOT_FOUND(40421, "AGENT_CARD_NOT_FOUND", "Agent card not found"),
    AGENT_SERVICE_UNAVAILABLE(50321, "AGENT_SERVICE_UNAVAILABLE", "Agent service is unavailable"),

    WORKSPACE_NOT_FOUND(40402, "WORKSPACE_NOT_FOUND", "Workspace not found"),
    ARTIFACT_NOT_FOUND(40403, "ARTIFACT_NOT_FOUND", "Artifact not found"),
    ARTIFACT_INVALID(40011, "ARTIFACT_INVALID", "Artifact is invalid"),
    WORKSPACE_STATE_INVALID(40913, "WORKSPACE_STATE_INVALID", "Workspace state is invalid"),
    STAGE_NOT_READY(40914, "STAGE_NOT_READY", "Required previous stage artifact is not present");

    private final int code;

    private final String errorCode;

    private final String message;
}