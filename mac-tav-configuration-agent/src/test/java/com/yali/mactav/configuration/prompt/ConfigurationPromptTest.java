package com.yali.mactav.configuration.prompt;

import static org.junit.jupiter.api.Assertions.*;

import com.yali.mactav.agent.core.agent.AgentUtils;
import org.junit.jupiter.api.Test;

/**
 * Resource tests for the ConfigurationAgent prompt contract.
 */
class ConfigurationPromptTest {

    @Test
    void promptShouldContainConfigurationAgentContract() {
        String prompt = AgentUtils.loadInstruction("prompts/configuration-agent-prompt.md");
        String lower = prompt.toLowerCase();

        assertTrue(prompt.contains("ConfigurationResponseSchema"));
        assertTrue(prompt.contains("NetworkPlan"));
        assertTrue(prompt.contains("ConfigSet"));
        assertTrue(prompt.contains("commandBlocks"));
        assertTrue(prompt.contains("traceRefs"));
        assertTrue(lower.contains("rollbackcommands"));
        assertTrue(lower.contains("execute commands"));
        assertTrue(lower.contains("verification"));
        assertTrue(prompt.contains("LLM"));
        assertTrue(prompt.contains("RAG"));
        assertTrue(prompt.contains("TEMPLATE"));
    }
}
