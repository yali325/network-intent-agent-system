package com.yali.mactav.intent.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.intent.IntentTestFixtures;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.intent.NetworkIntent;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline parser tests for IntentResponseSchema -> NetworkIntent conversion.
 *
 * <p>The test does not call a model or write Workspace state.</p>
 */
class IntentResponseParserTest {

    private final IntentResponseParser parser = new IntentResponseParser();

    @Test
    void parseShouldFillContextAndConvertSchemaFields() {
        NetworkIntent intent = parser.parse(IntentTestFixtures.enterpriseSchema(), IntentTestFixtures.context());

        assertEquals(IntentTestFixtures.TASK_ID, intent.getTaskId());
        assertEquals(2, intent.getIntentVersion());
        assertEquals(IntentTestFixtures.TRACE_ID, intent.getTraceId());
        assertEquals(IntentTestFixtures.RAW_TEXT, intent.getRawText());
        assertEquals(StageStatus.SUCCESS, intent.getStageStatus());
        assertNotNull(intent.getCreateTime());

        assertEquals(3, intent.getSemanticIntentGraph().getNodes().size());
        assertEquals(3, intent.getSemanticIntentGraph().getRelations().size());
        assertEquals("node-office", intent.getSemanticIntentGraph().getNodes().get(0).getId());
        assertEquals("rel-office-server", intent.getSemanticIntentGraph().getRelations().get(0).getId());
        assertEquals("ALLOW", intent.getSemanticIntentGraph().getRelations().get(0).getAction());
        assertEquals(1, intent.getAssumptions().size());
        assertEquals(1, intent.getConstraints().size());
        assertEquals(1, intent.getPreferences().size());
        assertEquals("OSPF", intent.getPreferences().get(0).getValue());
    }

    @Test
    void parseShouldNormalizeMissingOptionalListsToEmptyLists() {
        IntentResponseSchema schema = IntentResponseSchema.builder()
                .nodes(null)
                .relations(null)
                .assumptions(null)
                .constraints(null)
                .preferences(null)
                .warnings(null)
                .build();

        NetworkIntent intent = parser.parse(schema, IntentTestFixtures.context());

        assertEquals(List.of(), intent.getSemanticIntentGraph().getNodes());
        assertEquals(List.of(), intent.getSemanticIntentGraph().getRelations());
        assertTrue(intent.getAssumptions().isEmpty());
        assertTrue(intent.getConstraints().isEmpty());
        assertTrue(intent.getPreferences().isEmpty());
    }
}
