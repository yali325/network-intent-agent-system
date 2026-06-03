package com.yali.mactav.verification.prompt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.agent.core.agent.AgentUtils;
import org.junit.jupiter.api.Test;

/**
 * Offline prompt tests for the VerificationAgent instruction resource.
 */
class VerificationPromptTest {

    @Test
    void promptShouldDeclareVerificationBoundary() {
        String prompt = AgentUtils.loadInstruction("prompts/verification-agent-prompt.md");

        assertTrue(prompt.contains("ValidationReport"));
        assertTrue(prompt.contains("MUST NOT"));
        assertTrue(prompt.contains("Execute shell"));
    }
}
