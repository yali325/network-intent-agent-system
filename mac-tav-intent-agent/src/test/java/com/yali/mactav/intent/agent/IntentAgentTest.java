package com.yali.mactav.intent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.agent.core.agent.SchemaAgentInvoker;
import com.yali.mactav.intent.IntentTestFixtures;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.service.IntentServiceImpl;
import com.yali.mactav.model.intent.NetworkIntent;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for the IntentAgent wrapper orchestration boundary.
 *
 * <p>The test injects a SchemaAgentInvoker test fixture only to avoid real model
 * calls. It does not create a fake production agent or mock tool.</p>
 */
class IntentAgentTest {

    @Test
    void runShouldCallSchemaBoundaryThenIntentService() {
        SchemaAgentInvoker invoker = new SchemaAgentInvoker() {
            @Override
            public <T> T call(String input, Class<T> outputType) {
                return outputType.cast(IntentTestFixtures.enterpriseSchema());
            }
        };
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        IntentAgent intentAgent = new IntentAgent(invoker, objectMapper, new IntentServiceImpl());

        NetworkIntent intent = intentAgent.run(IntentTestFixtures.request());

        assertEquals(IntentTestFixtures.TASK_ID, intent.getTaskId());
        assertEquals(3, intent.getSemanticIntentGraph().getNodes().size());
        assertEquals("OSPF", intent.getPreferences().get(0).getValue());
    }
}
