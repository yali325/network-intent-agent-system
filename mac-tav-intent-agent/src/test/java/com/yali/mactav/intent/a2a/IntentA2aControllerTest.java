package com.yali.mactav.intent.a2a;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.agent.core.agent.SchemaAgentInvoker;
import com.yali.mactav.intent.IntentTestFixtures;
import com.yali.mactav.intent.agent.IntentAgent;
import com.yali.mactav.intent.service.IntentServiceImpl;
import com.yali.mactav.model.a2a.A2aRequest;
import com.yali.mactav.model.a2a.A2aResponse;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.intent.IntentAgentInvokePayload;
import com.yali.mactav.model.intent.NetworkIntent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for the internal IntentAgent A2A envelope endpoint.
 *
 * <p>The test uses a SchemaAgentInvoker fixture only at the model-call boundary
 * so no real model, Nacos, or production fake agent is involved.</p>
 */
class IntentA2aControllerTest {

    @Test
    void invokeShouldReturnNetworkIntentEnvelope() throws Exception {
        ObjectMapper objectMapper = objectMapper();
        SchemaAgentInvoker invoker = new SchemaAgentInvoker() {
            @Override
            public <T> T call(String input, Class<T> outputType) {
                return outputType.cast(IntentTestFixtures.enterpriseSchema());
            }
        };
        IntentAgent intentAgent = new IntentAgent(invoker, objectMapper, new IntentServiceImpl());
        IntentA2aController controller = new IntentA2aController(
                intentAgent,
                objectMapper,
                new IntentAgentInvokePayloadMapper());
        IntentAgentInvokePayload payload = IntentAgentInvokePayload.builder()
                .taskId(IntentTestFixtures.TASK_ID)
                .rawText(IntentTestFixtures.RAW_TEXT)
                .intentVersion(2)
                .traceId(IntentTestFixtures.TRACE_ID)
                .workspaceSnapshot("{}")
                .targetEnvironmentHint("lab")
                .createdBy("unit-test")
                .build();
        A2aRequest request = A2aRequest.builder()
                .taskId(IntentTestFixtures.TASK_ID)
                .sourceAgent("Orchestrator")
                .targetAgent("IntentAgent")
                .stage(WorkflowStage.INTENT)
                .artifactVersion(2)
                .payloadJson(objectMapper.writeValueAsString(payload))
                .traceId(IntentTestFixtures.TRACE_ID)
                .timestamp(LocalDateTime.now())
                .build();

        A2aResponse response = controller.invoke(request);

        assertTrue(response.getSuccess());
        assertEquals("IntentAgent", response.getSourceAgent());
        assertEquals("Orchestrator", response.getTargetAgent());
        NetworkIntent intent = objectMapper.readValue(response.getPayloadJson(), NetworkIntent.class);
        assertEquals(IntentTestFixtures.TASK_ID, intent.getTaskId());
        assertEquals(3, intent.getSemanticIntentGraph().getNodes().size());
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
