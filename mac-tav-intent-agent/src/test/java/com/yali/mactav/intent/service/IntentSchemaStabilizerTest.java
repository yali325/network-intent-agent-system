package com.yali.mactav.intent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.intent.IntentTestFixtures;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentNodeSchema;
import com.yali.mactav.intent.tool.IntentExtractTool;
import com.yali.mactav.model.intent.NetworkIntent;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for IntentSchemaStabilizer business-intent normalization.
 */
class IntentSchemaStabilizerTest {

    private final IntentExtractTool tool = new IntentExtractTool();

    private final IntentSchemaStabilizer stabilizer = new IntentSchemaStabilizer();

    private final IntentService service = new IntentServiceImpl();

    @Test
    void stabilizeShouldFillRelationsFromToolHintsWhenModelRelationsAreEmpty() {
        IntentResponseSchema modelSchema = IntentResponseSchema.builder()
                .nodes(List.of(
                        IntentNodeSchema.builder().id("node-office").name("office").type("ZONE").build(),
                        IntentNodeSchema.builder().id("node-guest").name("guest").type("ZONE").build(),
                        IntentNodeSchema.builder().id("node-server").name("server").type("SERVICE").build()))
                .relations(List.of())
                .build();

        IntentResponseSchema stabilized = stabilizer.stabilize(
                IntentTestFixtures.RAW_TEXT,
                modelSchema,
                tool.extractIntentHints(IntentTestFixtures.RAW_TEXT));

        assertEquals(3, stabilized.getRelations().size());
        assertTrue(stabilized.getRelations().stream().anyMatch(relation ->
                relation.getId().equals("rel-office-guest")
                        && relation.getType().equals("ISOLATION")
                        && relation.getAction().equals("DENY")));
    }

    @Test
    void stabilizeShouldBuildMinimumSchemaFromToolHintsWhenModelSchemaIsNull() {
        String rawText = "办公区与访客区隔离，访客区不能访问服务器区。";

        IntentResponseSchema stabilized = stabilizer.stabilize(
                rawText,
                null,
                tool.extractIntentHints(rawText));

        assertEquals(3, stabilized.getNodes().size());
        assertEquals(2, stabilized.getRelations().size());
        assertTrue(stabilized.getRelations().stream().anyMatch(relation ->
                relation.getId().equals("rel-guest-server")
                        && relation.getSource().equals("node-guest")
                        && relation.getTarget().equals("node-server")
                        && relation.getAction().equals("DENY")));
    }

    @Test
    void stabilizeShouldRemainInsideIntentBoundaryAndPassValidator() {
        IntentResponseSchema stabilized = stabilizer.stabilize(
                IntentTestFixtures.RAW_TEXT,
                null,
                tool.extractIntentHints(IntentTestFixtures.RAW_TEXT));

        String output = stabilized.toString().toLowerCase();
        assertFalse(output.contains("vlan"));
        assertFalse(output.contains("acl"));
        assertFalse(output.contains("cli"));
        assertFalse(output.contains("10.1."));
        assertFalse(output.contains("interface"));

        NetworkIntent intent = service.parse(stabilized, IntentTestFixtures.request());
        assertNotNull(intent);
        assertEquals(3, intent.getSemanticIntentGraph().getNodes().size());
        assertEquals(3, intent.getSemanticIntentGraph().getRelations().size());
    }
}
