package com.yali.mactav.agent.core.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class PromptLoaderTest {

    private final PromptLoader promptLoader = new PromptLoader();

    @Test
    void loadFromClasspathShouldReturnPromptText() {
        String prompt = promptLoader.loadFromClasspath("prompts/core-test-prompt.md");

        assertTrue(prompt.contains("MAC-TAV test prompt"));
    }

    @Test
    void loadFromClasspathShouldThrowBusinessExceptionWhenMissing() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> promptLoader.loadFromClasspath("prompts/not-found.md")
        );

        assertEquals(ErrorCode.PROMPT_NOT_FOUND.getErrorCode(), exception.getErrorCode());
    }
}
