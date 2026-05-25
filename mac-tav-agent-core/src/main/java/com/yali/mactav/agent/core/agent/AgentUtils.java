package com.yali.mactav.agent.core.agent;

import com.yali.mactav.agent.core.prompt.PromptLoader;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;

/**
 * Shared utility facade for future real MAC-TAV agents.
 *
 * <p>This class belongs to mac-tav-agent-core. It may load instructions and wrap
 * agent/model/parser/tool failures, but it must not contain business prompts,
 * orchestrator remote-client logic, or concrete agent implementations.</p>
 */
public final class AgentUtils {

    private static final PromptLoader PROMPT_LOADER = new PromptLoader();

    private AgentUtils() {
    }

    public static String loadInstruction(String path) {
        return PROMPT_LOADER.loadFromClasspath(path);
    }

    public static <T> T callSchema(SchemaAgentInvoker invoker, String input, Class<T> outputType) {
        if (invoker == null) {
            throw new BusinessException(ErrorCode.AGENT_EXECUTION_FAILED, "Schema agent invoker must not be null");
        }
        try {
            return invoker.call(input, outputType);
        }
        catch (BusinessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw wrapException(
                    ErrorCode.AGENT_EXECUTION_FAILED.getErrorCode(),
                    "Agent schema call failed",
                    ex
            );
        }
    }

    public static BusinessException wrapException(String errorCode, String message, Throwable cause) {
        if (cause instanceof BusinessException businessException) {
            return businessException;
        }
        return new BusinessException(errorCode, message, cause);
    }

    public static BusinessException wrapException(ErrorCode errorCode, String message, Throwable cause) {
        return wrapException(errorCode.getErrorCode(), message, cause);
    }

    public static BusinessException modelCallFailed(String message, Throwable cause) {
        return wrapException(ErrorCode.MODEL_CALL_FAILED, message, cause);
    }

    public static BusinessException parseFailed(String message, Throwable cause) {
        return wrapException(ErrorCode.AGENT_PARSE_FAILED, message, cause);
    }

    public static BusinessException validationFailed(String message, Throwable cause) {
        return wrapException(ErrorCode.AGENT_OUTPUT_INVALID, message, cause);
    }

    public static BusinessException toolCallFailed(String message, Throwable cause) {
        return wrapException(ErrorCode.TOOL_CALL_FAILED, message, cause);
    }

    /*
     * TODO Phase 1+: add ReactAgent/ChatModel overloads after the exact Spring AI Alibaba
     * Agent API is confirmed in project dependencies. This keeps the public utility class
     * stable without inventing non-compilable framework calls in the skeleton phase.
     */
}
