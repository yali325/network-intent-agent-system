package com.yali.mactav.planning.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MockPlanningAgentTest {

    @Test
    void mockPlanningOutputsExpectedTopologyNodes() {
        NetworkIntent intent = NetworkIntent.builder()
                .taskId("task-10001")
                .intentVersion(1)
                .build();

        NetworkPlan plan = new MockPlanningAgent()
                .execute(AgentContext.of("task-10001", null), intent)
                .getData();

        Set<String> nodeIds = plan.getTopology().getNodes().stream()
                .map(node -> node.getId())
                .collect(Collectors.toSet());
        assertTrue(nodeIds.containsAll(Set.of("R1", "SW1", "SW2", "office-pc-1", "guest-pc-1", "server-1")));
        assertEquals("routing-ospf", plan.getRoutingPlan().getId());
        assertTrue(plan.getAddressPlan().stream().allMatch(item -> item.getId() != null));
        assertTrue(plan.getVlanPlan().stream().allMatch(item -> item.getId() != null));
        assertTrue(plan.getSecurityPolicyPlan().stream().allMatch(item -> item.getId() != null));
    }
}
