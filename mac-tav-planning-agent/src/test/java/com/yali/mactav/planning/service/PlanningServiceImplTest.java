package com.yali.mactav.planning.service;

import static org.junit.jupiter.api.Assertions.*;

import com.yali.mactav.agent.core.validator.AgentValidationException;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.planning.PlanningTestFixtures;
import com.yali.mactav.planning.schema.PlanningResponseSchema;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline service tests for the PlanningAgent Parser -> Validator chain.
 *
 * <p>The service test does not call a model and does not use the service as an
 * Orchestrator local-agent integration path.</p>
 */
class PlanningServiceImplTest {

    private final PlanningService service = new PlanningServiceImpl();

    @Test
    void parseShouldRunParserThenValidator() {
        NetworkPlan plan = service.parse(PlanningTestFixtures.enterprisePlanSchema(),
                PlanningTestFixtures.request());

        assertEquals(PlanningTestFixtures.TASK_ID, plan.getTaskId());
        assertEquals(5, plan.getTopology().getNodes().size());
        assertEquals(4, plan.getTopology().getLinks().size());
        assertEquals(3, plan.getSecurityPolicyPlan().size());
        assertNotNull(plan.getRoutingPlan());
    }

    @Test
    void parseShouldThrowWhenParsedPlanIsInvalid() {
        PlanningResponseSchema schema = PlanningResponseSchema.builder()
                .topologyNodes(List.of())
                .topologyLinks(List.of())
                .build();

        assertThrows(AgentValidationException.class,
                () -> service.parse(schema, PlanningTestFixtures.request()));
    }
}
