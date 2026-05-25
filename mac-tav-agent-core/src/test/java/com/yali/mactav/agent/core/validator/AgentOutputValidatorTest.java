package com.yali.mactav.agent.core.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.enums.ErrorCode;
import org.junit.jupiter.api.Test;

class AgentOutputValidatorTest {

    private final AgentOutputValidator<String> notBlankValidator = output -> {
        if (output == null || output.isBlank()) {
            return ValidationResult.fail("output must not be blank");
        }
        return ValidationResult.ok();
    };

    @Test
    void validateAndReturnShouldReturnValidOutput() {
        String output = "valid-output";

        String actual = notBlankValidator.validateAndReturn(output);

        assertSame(output, actual);
    }

    @Test
    void validateAndReturnShouldThrowValidationExceptionWhenInvalid() {
        AgentValidationException exception = assertThrows(
                AgentValidationException.class,
                () -> notBlankValidator.validateAndReturn(" ")
        );

        assertEquals(ErrorCode.AGENT_OUTPUT_INVALID.getErrorCode(), exception.getErrorCode());
        assertTrue(exception.getValidationResult().getMessages().contains("output must not be blank"));
    }
}
