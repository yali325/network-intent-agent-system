package com.yali.mactav.planning.prompt;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Validates that the PlanningAgent prompt file exists and is loadable.
 */
class PlanningPromptTest {

    @Test
    void promptFileShouldExistAndContainKeyInstructions() throws IOException {
        // The prompt is loaded from classpath by PromptLoader in AgentUtils.
        // This test validates the file exists at its expected location.
        try (var stream = getClass().getClassLoader()
                .getResourceAsStream("prompts/planning-agent-prompt.md")) {

            assertNotNull(stream, "planning-agent-prompt.md must exist in classpath");
            String content = new String(stream.readAllBytes());
            assertTrue(content.contains("PlanningAgent"));
            assertTrue(content.contains("PlanningResponseSchema"));
            assertTrue(content.contains("MUST NOT output executable CLI"));
            assertTrue(content.contains("Routing Contract"));
            assertTrue(content.contains("routingPlan.routers must contain at least one router"));
        }
    }
}
