package com.yali.mactav.orchestrator.remote.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for safe A2A failure summarization.
 */
class OfficialA2aClientErrorSummaryTest {

    private final OfficialA2aClient client = new OfficialA2aClient(null, new ObjectMapper());

    @Test
    void summarizeShouldRedactSensitiveRequestContentWhenNoSafeRemoteErrorExists() {
        String summary = client.summarizeA2aFailureForTest(
                "{\"method\":\"message/send\",\"params\":{\"payloadJson\":\"secret\",\"rawText\":\"operator intent\"}}");

        assertTrue(summary.contains("[SAA_A2A_CONTENT_REDACTED"));
        assertFalse(summary.contains("payloadJson"));
        assertFalse(summary.contains("rawText"));
        assertFalse(summary.contains("operator intent"));
    }

    @Test
    void summarizeShouldPreserveRemoteBusinessErrorWithoutLeakingPayload() {
        String summary = client.summarizeA2aFailureForTest(
                "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32602,\"message\":\"AGENT_OUTPUT_INVALID: routingPlan.routers must not be empty\",\"data\":{\"payloadJson\":\"secret\"}}}");

        assertTrue(summary.contains("errorCode=-32602"));
        assertTrue(summary.contains("routingPlan.routers must not be empty"));
        assertFalse(summary.contains("payloadJson"));
        assertFalse(summary.contains("secret"));
    }

    @Test
    void summarizeShouldExtractEscapedRemoteErrorAndRedactUnsafeFields() {
        String summary = client.summarizeA2aFailureForTest(
                "java.lang.RuntimeException: {\\\"errorCode\\\":\\\"AGENT_OUTPUT_INVALID\\\",\\\"errorMessage\\\":\\\"routingPlan.traceRefs must include at least one intentNodeId\\\",\\\"workspaceSnapshot\\\":\\\"secret\\\",\\\"prompt\\\":\\\"secret\\\"}");

        assertTrue(summary.contains("errorCode=AGENT_OUTPUT_INVALID"));
        assertTrue(summary.contains("routingPlan.traceRefs must include at least one intentNodeId"));
        assertFalse(summary.contains("workspaceSnapshot"));
        assertFalse(summary.contains("prompt"));
        assertFalse(summary.contains("secret"));
    }

    @Test
    void summarizeShouldExtractKnownProjectErrorFromNonStandardText() {
        String summary = client.summarizeA2aFailureForTest(
                "task failed text AGENT_SCHEMA_INVALID: Agent response cannot be parsed as schema, payload follows {\\\"payloadJson\\\":\\\"secret\\\",\\\"rawText\\\":\\\"secret\\\"}");

        assertTrue(summary.contains("errorCode=AGENT_SCHEMA_INVALID"));
        assertTrue(summary.contains("Agent response cannot be parsed as schema"));
        assertFalse(summary.contains("payloadJson"));
        assertFalse(summary.contains("rawText"));
        assertFalse(summary.contains("secret"));
    }
}
