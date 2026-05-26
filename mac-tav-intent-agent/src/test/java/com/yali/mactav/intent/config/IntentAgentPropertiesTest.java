package com.yali.mactav.intent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for IntentAgent configuration defaults and key-name resolution.
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

    @Test
    void apiKeyResolverShouldSupportOnlyDashedKeyName() {
        assertEquals(
                "test-key",
                IntentAgentApiKeyResolver.resolve(Map.of(
                        "unused.key", "ignored-key",
                        IntentAgentApiKeyResolver.API_KEY_ENV_NAME, "test-key")).orElseThrow()
        );
    }

    @Test
    void apiKeyResolverShouldIgnoreOtherNames() {
        assertTrue(IntentAgentApiKeyResolver.resolve(
                Map.of("unused.key", "ignored-key", "anotherKey", "ignored-too")).isEmpty());
    }
}
