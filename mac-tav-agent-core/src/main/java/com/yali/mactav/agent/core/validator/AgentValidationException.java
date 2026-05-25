package com.yali.mactav.agent.core.validator;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import lombok.Getter;

/**
 * Unified validation failure raised by AgentOutputValidator implementations.
 *
 * <p>The exception maps invalid agent output to AGENT_OUTPUT_INVALID without
 * leaking model-provider or concrete agent implementation details.</p>
 */
@Getter
public class AgentValidationException extends BusinessException {

    private final ValidationResult validationResult;

    public AgentValidationException(ValidationResult validationResult) {
        super(ErrorCode.AGENT_OUTPUT_INVALID, buildMessage(validationResult));
        this.validationResult = validationResult;
    }

    private static String buildMessage(ValidationResult validationResult) {
        if (validationResult == null || validationResult.getMessages().isEmpty()) {
            return ErrorCode.AGENT_OUTPUT_INVALID.getMessage();
        }
        return String.join("; ", validationResult.getMessages());
    }
}
