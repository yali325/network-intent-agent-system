package com.yali.mactav.agent.core.agent;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.prompt.PromptLoader;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import java.util.Objects;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;

public final class AgentUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private AgentUtils() {
    }

    public static Builder reactAgentBuilder(String name, String description, ChatModel chatModel) {
        if (isBlank(name)) {
            throw wrapException(ErrorCode.BAD_REQUEST.name(), "Agent name must not be blank", null);
        }
        if (chatModel == null) {
            throw wrapException(ErrorCode.MODEL_CALL_FAILED.name(), "ChatModel must not be null", null);
        }
        return ReactAgent.builder()
                .name(name)
                .description(description)
                .model(chatModel)
                .enableLogging(true);
    }

    public static <T> T callSchema(ReactAgent agent, String input, Class<T> outputType) {
        Objects.requireNonNull(agent, "agent must not be null");
        return callSchema(value -> {
            AssistantMessage message = agent.call(value);
            return message == null ? null : message.getText();
        }, input, outputType);
    }

    public static <T> T callSchema(AgentInvoker invoker, String input, Class<T> outputType) {
        Objects.requireNonNull(invoker, "invoker must not be null");
        Objects.requireNonNull(outputType, "outputType must not be null");
        try {
            String output = invoker.call(input);
            return parseSchema(output, outputType);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw wrapException(
                    ErrorCode.MODEL_CALL_FAILED.name(),
                    "Agent schema call failed",
                    ex
            );
        }
    }

    public static String loadInstruction(String path) {
        return PromptLoader.load(path);
    }

    public static BusinessException wrapException(String errorCode, String message, Throwable cause) {
        ErrorCode resolved = ErrorCode.from(errorCode, ErrorCode.PIPELINE_FAILED);
        String safeMessage = isBlank(message) ? resolved.name() : message;
        return new BusinessException(resolved, safeMessage, cause);
    }

    private static <T> T parseSchema(String output, Class<T> outputType) {
        if (outputType == String.class) {
            return outputType.cast(output);
        }
        if (isBlank(output)) {
            throw wrapException(
                    ErrorCode.AGENT_SCHEMA_INVALID.name(),
                    "Agent output is empty",
                    null
            );
        }
        try {
            return OBJECT_MAPPER.readValue(extractJson(output), outputType);
        } catch (JsonProcessingException ex) {
            throw wrapException(
                    ErrorCode.AGENT_SCHEMA_INVALID.name(),
                    "Agent output does not match schema: " + readableMessage(ex),
                    ex
            );
        }
    }

    private static String extractJson(String output) {
        String text = stripMarkdownFence(output.trim());
        int start = firstJsonStart(text);
        if (start < 0) {
            throw wrapException(
                    ErrorCode.AGENT_SCHEMA_INVALID.name(),
                    "Agent output does not contain JSON",
                    null
            );
        }

        char opening = text.charAt(start);
        char closing = opening == '{' ? '}' : ']';
        int end = matchingJsonEnd(text, start, opening, closing);
        if (end < 0) {
            throw wrapException(
                    ErrorCode.AGENT_SCHEMA_INVALID.name(),
                    "Agent output contains incomplete JSON",
                    null
            );
        }
        return text.substring(start, end + 1);
    }

    private static String stripMarkdownFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        String withoutOpening = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return withoutOpening.replaceFirst("\\s*```$", "").trim();
    }

    private static int firstJsonStart(String text) {
        int objectStart = text.indexOf('{');
        int arrayStart = text.indexOf('[');
        if (objectStart < 0) {
            return arrayStart;
        }
        if (arrayStart < 0) {
            return objectStart;
        }
        return Math.min(objectStart, arrayStart);
    }

    private static int matchingJsonEnd(String text, int start, char opening, char closing) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = inString;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == opening) {
                depth++;
            } else if (current == closing) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static String readableMessage(JsonProcessingException ex) {
        if (ex.getOriginalMessage() == null || ex.getOriginalMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getOriginalMessage();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
