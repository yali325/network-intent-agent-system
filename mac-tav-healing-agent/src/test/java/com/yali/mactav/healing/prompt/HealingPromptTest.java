package com.yali.mactav.healing.prompt;

import com.yali.mactav.agent.core.agent.AgentUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the HealingAgent prompt is packaged and boundary-safe.
 */
class HealingPromptTest {

    @Test
    void promptExistsAndForbidsDirectExecution() {
        String prompt = AgentUtils.loadInstruction("prompts/healing-agent-prompt.md");

        assertFalse(prompt.isBlank());
        assertTrue(prompt.contains("Do not claim that you modified NetworkWorkspace"));
        assertTrue(prompt.contains("Do not output arbitrary shell"));
        assertTrue(prompt.contains("Orchestrator"));
    }
}
