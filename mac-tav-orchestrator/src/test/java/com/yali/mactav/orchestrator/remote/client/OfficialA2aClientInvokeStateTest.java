package com.yali.mactav.orchestrator.remote.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the SAA A2aRemoteAgent invoke state mapping.
 */
class OfficialA2aClientInvokeStateTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final OfficialA2aClient client = new OfficialA2aClient(null, objectMapper);

    @Test
    void buildInvokeStateShouldPutSerializedA2aRequestUnderInputKey() throws Exception {
        A2aRequest request = A2aRequest.builder()
                .taskId("task-a2a-state")
                .sourceAgent("MacTavOrchestrator")
                .targetAgent("IntentAgent")
                .stage(WorkflowStage.INTENT)
                .artifactVersion(1)
                .payloadJson("{\"rawText\":\"office guest isolation\",\"workspaceSnapshot\":\"compact\"}")
                .traceId("trace-a2a-state")
                .timestamp(LocalDateTime.of(2026, 6, 14, 15, 30))
                .build();
        String input = objectMapper.writeValueAsString(request);

        Map<String, Object> state = client.buildInvokeState(input);

        assertEquals(1, state.size());
        assertTrue(state.containsKey("input"));
        assertInstanceOf(String.class, state.get("input"));
        String stateInput = state.get("input").toString();
        assertFalse(stateInput.isBlank());
        assertTrue(stateInput.contains("\"payloadJson\""));
        assertTrue(stateInput.contains("\"rawText\""));
        assertTrue(stateInput.contains("\"workspaceSnapshot\""));

        A2aRequest decoded = objectMapper.readValue(stateInput, A2aRequest.class);
        assertEquals("task-a2a-state", decoded.getTaskId());
        assertEquals("IntentAgent", decoded.getTargetAgent());
        assertEquals(WorkflowStage.INTENT, decoded.getStage());
        assertEquals("{\"rawText\":\"office guest isolation\",\"workspaceSnapshot\":\"compact\"}",
                decoded.getPayloadJson());
        assertEquals("trace-a2a-state", decoded.getTraceId());
    }

    @Test
    void buildInvokeStateShouldRejectBlankInputBeforeSaaCall() {
        BusinessException error = assertThrows(BusinessException.class, () -> client.buildInvokeState("  "));

        assertEquals(ErrorCode.BAD_REQUEST, error.getErrorCode());
        assertTrue(error.getMessage().contains("must not be blank"));
    }
}
