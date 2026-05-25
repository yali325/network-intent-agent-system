package com.yali.mactav.intent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.agent.core.validator.AgentValidationException;
import com.yali.mactav.intent.IntentTestFixtures;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.model.intent.NetworkIntent;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline service tests for the IntentAgent Parser -> Validator chain.
 *
 * <p>The service test does not call a model and does not use the service as an
 * Orchestrator local-agent integration path.</p>
 */
class IntentServiceImplTest {

    private final IntentService service = new IntentServiceImpl();

    @Test
    void parseShouldRunParserThenValidator() {
        NetworkIntent intent = service.parse(IntentTestFixtures.enterpriseSchema(), IntentTestFixtures.request());

        assertEquals(IntentTestFixtures.TASK_ID, intent.getTaskId());
        assertEquals(3, intent.getSemanticIntentGraph().getNodes().size());
        assertEquals(3, intent.getSemanticIntentGraph().getRelations().size());
    }

    @Test
    void parseShouldThrowWhenParsedIntentIsInvalid() {
        IntentResponseSchema schema = IntentResponseSchema.builder()
                .nodes(List.of())
                .relations(List.of())
                .build();

        assertThrows(AgentValidationException.class, () -> service.parse(schema, IntentTestFixtures.request()));
    }
}
