package com.yali.mactav.agent.core.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class LlmJsonParser {

    private final ObjectMapper objectMapper;

    public LlmJsonParser() {
        this(new ObjectMapper());
    }

    public LlmJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public <T> T parseObject(String llmOutput, Class<T> targetType) {
        String json = extractJson(llmOutput);
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                    ErrorCode.PIPELINE_FAILED,
                    "LLM JSON parse failed: " + readableMessage(ex)
            );
        }
    }

    public String extractJson(String llmOutput) {
        if (llmOutput == null || llmOutput.isBlank()) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "LLM output is empty");
        }

        String text = stripMarkdownFence(llmOutput.trim());
        int start = firstJsonStart(text);
        if (start < 0) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "LLM output does not contain JSON");
        }

        char opening = text.charAt(start);
        char closing = opening == '{' ? '}' : ']';
        int end = matchingJsonEnd(text, start, opening, closing);
        if (end < 0) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "LLM output contains incomplete JSON");
        }
        return text.substring(start, end + 1);
    }

    private String stripMarkdownFence(String text) {
        if (!text.startsWith("```")) {
            return text;
        }
        String withoutOpening = text.replaceFirst("^```[a-zA-Z]*\\s*", "");
        return withoutOpening.replaceFirst("\\s*```$", "").trim();
    }

    private int firstJsonStart(String text) {
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

    private int matchingJsonEnd(String text, int start, char opening, char closing) {
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

    private String readableMessage(JsonProcessingException ex) {
        if (ex.getOriginalMessage() == null || ex.getOriginalMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getOriginalMessage();
    }
}
