package com.yali.mactav.intent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Offline tests for IntentAgent configuration defaults.
 *
 * <p>The tests do not require real API keys and do not create a ChatModel.</p>
 */
class IntentAgentPropertiesTest {

    @Test
    void propertiesShouldExposeSafeDefaults() {
        IntentAgentProperties properties = new IntentAgentProperties();

        assertTrue(properties.isEnabled());
        assertEquals(6, properties.effectiveRunLimit());
        assertEquals("prompts/intent-agent-prompt.md", properties.effectivePromptPath());
    }
}
