package com.yali.mactav.healing.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies HealingAgent boundary checks that do not call external models.
 */
class HealingAgentBoundaryTest {

    @Test
    void constructorRejectsMissingReactAgent() {
        assertThrows(NullPointerException.class, () -> new HealingAgent(null, null, null));
    }
}
