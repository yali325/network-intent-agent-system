package com.yali.mactav.intent.agent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.intent.SemanticIntentGraph;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmIntentAgentTest {

    @Test
    void validatesSemanticIntentGraph() {
        NetworkIntent intent = validIntent();

        assertDoesNotThrow(() -> validator().validateIntent(intent));
    }

    @Test
    void validationRejectsMissingRelationSourceOrTarget() {
        NetworkIntent intent = validIntent();
        intent.getSemanticIntentGraph().getRelations().get(0).setTarget("missing-server");

        assertThrows(BusinessException.class, () -> validator().validateIntent(intent));
    }

    @Test
    void validationRejectsConcreteNetworkDesign() {
        NetworkIntent intent = validIntent();
        intent.getSemanticIntentGraph().getNodes().get(0).setDescription("Use VLAN 10 for office");

        assertThrows(BusinessException.class, () -> validator().validateIntent(intent));
    }

    private LlmIntentAgent validator() {
        return new LlmIntentAgent(null, null, null);
    }

    private NetworkIntent validIntent() {
        return NetworkIntent.builder()
                .taskId("task-llm-001")
                .intentVersion(1)
                .rawText("office can access server")
                .semanticIntentGraph(SemanticIntentGraph.builder()
                        .nodes(List.of(
                                node("office", "Office", "ZONE"),
                                node("server", "Server", "ZONE")
                        ))
                        .relations(List.of(IntentRelation.builder()
                                .id("rel-001")
                                .type("ACCESS")
                                .source("office")
                                .target("server")
                                .action("ALLOW")
                                .service("ANY")
                                .explicit(true)
                                .build()))
                        .build())
                .build();
    }

    private IntentNode node(String id, String name, String type) {
        return IntentNode.builder()
                .id(id)
                .name(name)
                .type(type)
                .build();
    }
}
