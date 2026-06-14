package com.yali.mactav.agent.core.agent;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.agent.core.prompt.PromptLoader;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Shared utility facade for future real MAC-TAV agents.
 *
 * <p>This class belongs to mac-tav-agent-core. It may load instructions and wrap
 * agent/model/parser/tool failures, but it must not contain business prompts,
 * orchestrator remote-client logic, or concrete agent implementations.</p>
 */
public final class AgentUtils {

    private static final PromptLoader PROMPT_LOADER = new PromptLoader();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private AgentUtils() {
    }

    public static Builder reactAgentBuilder(String name, String description, ChatModel chatModel) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.AGENT_EXECUTION_FAILED, "Agent name must not be blank");
        }
        if (chatModel == null) {
            throw new BusinessException(ErrorCode.MODEL_CALL_FAILED, "ChatModel must not be null");
        }
        return ReactAgent.builder()
                .name(name)
                .description(description)
                .model(chatModel)
                .enableLogging(verboseLoggingEnabled());
    }

    private static boolean verboseLoggingEnabled() {
        String property = System.getProperty("mactav.agent.verbose-logging");
        if (property == null || property.isBlank()) {
            property = System.getenv("MACTAV_AGENT_VERBOSE_LOGGING");
        }
        return Boolean.parseBoolean(property);
    }

    public static String loadInstruction(String path) {
        return PROMPT_LOADER.loadFromClasspath(path);
    }

    public static <T> T callSchema(ReactAgent agent, String input, Class<T> outputType) {
        if (agent == null) {
            throw new BusinessException(ErrorCode.AGENT_EXECUTION_FAILED, "ReactAgent must not be null");
        }
        if (outputType == null) {
            throw new BusinessException(ErrorCode.AGENT_SCHEMA_INVALID, "Output type must not be null");
        }

        AssistantMessage response;
        try {
            response = agent.call(input);
        }
        catch (BusinessException ex) {
            throw ex;
        }
        catch (Exception ex) {
            throw modelCallFailed("ReactAgent schema call failed", ex);
        }
        return parseAssistantMessage(response, outputType);
    }

    private static <T> T parseAssistantMessage(AssistantMessage response, Class<T> outputType) {
        if (response == null || response.getText() == null || response.getText().isBlank()) {
            throw new BusinessException(ErrorCode.AGENT_SCHEMA_INVALID, "Agent response schema text must not be blank");
        }
        String text = response.getText();
        if (String.class.equals(outputType)) {
            return outputType.cast(text);
        }
        try {
            return OBJECT_MAPPER.readValue(extractJson(text), outputType);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.AGENT_SCHEMA_INVALID, "Agent response cannot be parsed as schema", ex);
        }
    }

    private static String extractJson(String text) {
        String normalized = stripMarkdownFence(text.trim());
        int objectStart = normalized.indexOf('{');
        int arrayStart = normalized.indexOf('[');
        int start;
        if (objectStart < 0) {
            start = arrayStart;
        }
        else if (arrayStart < 0) {
            start = objectStart;
        }
        else {
            start = Math.min(objectStart, arrayStart);
        }
        int objectEnd = normalized.lastIndexOf('}');
        int arrayEnd = normalized.lastIndexOf(']');
        int end = Math.max(objectEnd, arrayEnd);
        if (start >= 0 && end >= start) {
            return normalized.substring(start, end + 1);
        }
        return normalized;
    }

    private static String stripMarkdownFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        int firstLineEnd = text.indexOf('\n');
        int lastFence = text.lastIndexOf("```");
        if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
            return text.substring(firstLineEnd + 1, lastFence).trim();
        }
        return text;
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
     * TODO: enrich RunnableConfig/tool-context support after the project decides how
     * to pass trace metadata through Spring AI Alibaba graph execution.
     */
}
