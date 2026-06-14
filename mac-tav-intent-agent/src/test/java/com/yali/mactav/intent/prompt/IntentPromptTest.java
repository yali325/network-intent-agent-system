package com.yali.mactav.intent.prompt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.agent.core.prompt.PromptLoader;
import org.junit.jupiter.api.Test;

/**
 * Offline prompt resource test for the IntentAgent instruction file.
 *
 * <p>The test only verifies classpath loading and required boundary text; it
 * never calls a real model API.</p>
 */
class IntentPromptTest {

    private final PromptLoader promptLoader = new PromptLoader();

    @Test
    void intentPromptShouldBeLoadableFromClasspath() {
        String prompt = promptLoader.loadFromClasspath("prompts/intent-agent-prompt.md");

        assertTrue(prompt.contains("MAC-TAV IntentAgent"));
        assertTrue(prompt.contains("IntentResponseSchema"));
        assertTrue(prompt.contains("MUST NOT"));
        assertTrue(prompt.contains("VLAN"));
        assertTrue(prompt.contains("Relation Contract"));
        assertTrue(prompt.contains("relations field is mandatory"));
        assertTrue(prompt.contains("隔离"));
    }
}
