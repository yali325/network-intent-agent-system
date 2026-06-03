package com.yali.mactav.verification.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Offline tests for deterministic VerificationAgent fact helper behavior.
 */
class VerificationFactToolTest {

    @Test
    void shouldClassifyAllowAndIsolationExpectations() {
        VerificationFactTool tool = new VerificationFactTool();

        assertEquals("REACHABLE", tool.classifyConnectivityExpectation("ALLOW", "ACCESS").expectedResult());
        assertEquals("BLOCKED", tool.classifyConnectivityExpectation("ALLOW", "ISOLATION").expectedResult());
        assertEquals("BLOCKED", tool.classifyConnectivityExpectation("DENY", "ACCESS").expectedResult());
    }
}
