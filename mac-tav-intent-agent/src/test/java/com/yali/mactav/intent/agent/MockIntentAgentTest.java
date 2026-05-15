package com.yali.mactav.intent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.model.intent.NetworkIntent;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MockIntentAgentTest {

    @Test
    void mockIntentContainsOnlyBusinessIntent() {
        NetworkIntent intent = new MockIntentAgent()
                .execute(AgentContext.of("task-10001", "raw"), "raw")
                .getData();

        assertNotNull(intent.getSemanticIntentGraph());
        Set<String> nodeIds = intent.getSemanticIntentGraph().getNodes().stream()
                .map(node -> node.getId())
                .collect(Collectors.toSet());
        assertEquals(Set.of("office", "guest", "server", "internet"), nodeIds);
        assertTrue(intent.getSemanticIntentGraph().getNodes().stream()
                .noneMatch(node -> "DEVICE".equals(node.getType())));
        assertFalse(containsConcreteNetworkDesign(intent));
    }

    private boolean containsConcreteNetworkDesign(NetworkIntent intent) {
        String graphText = intent.getSemanticIntentGraph().toString();
        return graphText.contains("R1")
                || graphText.contains("SW1")
                || graphText.contains("SW2")
                || graphText.contains("VLAN")
                || graphText.contains("192.168")
                || graphText.contains("GE0/");
    }
}
