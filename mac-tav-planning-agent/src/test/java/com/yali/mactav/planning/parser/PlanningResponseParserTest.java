package com.yali.mactav.planning.parser;

import static org.junit.jupiter.api.Assertions.*;

import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.planning.PlanningTestFixtures;
import com.yali.mactav.planning.schema.PlanningResponseSchema;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline parser tests for PlanningResponseSchema -> NetworkPlan conversion.
 *
 * <p>The test does not call a model or write Workspace state.</p>
 */
class PlanningResponseParserTest {

    private final PlanningResponseParser parser = new PlanningResponseParser();

    @Test
    void parseShouldFillContextAndConvertSchemaFields() {
        NetworkPlan plan = parser.parse(PlanningTestFixtures.enterprisePlanSchema(),
                PlanningTestFixtures.context());

        assertEquals(PlanningTestFixtures.TASK_ID, plan.getTaskId());
        assertEquals(1, plan.getPlanVersion());
        assertEquals(1, plan.getIntentVersion());
        assertEquals(StageStatus.SUCCESS, plan.getStageStatus());
        assertNotNull(plan.getCreateTime());
        assertNotNull(plan.getUpdateTime());
        assertNotNull(plan.getCreatedBy());
        assertNotNull(plan.getPlanSummary());

        assertEquals(5, plan.getTopology().getNodes().size());
        assertEquals("sw-core", plan.getTopology().getNodes().get(0).getId());
        assertEquals(4, plan.getTopology().getLinks().size());
        assertEquals("link-core-office", plan.getTopology().getLinks().get(0).getId());

        assertEquals(4, plan.getZones().size());
        assertEquals("zone-office", plan.getZones().get(0).getId());
        assertEquals("node-office", plan.getZones().get(0).getMappedFromIntentNode());

        assertEquals(3, plan.getAddressPlan().size());
        assertEquals("10.1.0.0/24", plan.getAddressPlan().get(0).getSubnet());

        assertEquals(3, plan.getVlanPlan().size());
        assertEquals(100, plan.getVlanPlan().get(0).getVlanId());

        assertNotNull(plan.getRoutingPlan());
        assertEquals("OSPF", plan.getRoutingPlan().getProtocol());
        assertEquals("0.0.0.0", plan.getRoutingPlan().getArea());
        assertEquals(1, plan.getRoutingPlan().getRouters().size());

        assertEquals(3, plan.getSecurityPolicyPlan().size());
        assertEquals("sec-office-to-server", plan.getSecurityPolicyPlan().get(0).getId());
        assertEquals("ALLOW", plan.getSecurityPolicyPlan().get(0).getAction());

        assertNotNull(plan.getTraceRefs());
        assertFalse(plan.getTraceRefs().getIntentNodeIds().isEmpty());

        assertEquals(1, plan.getPlanConstraints().size());
        assertNull(plan.getNatPlan());
    }

    @Test
    void parseShouldSetCreatedByFromContext() {
        var ctx = PlanningTestFixtures.context();
        ctx.setCreatedBy("unit-test-user");
        NetworkPlan plan = parser.parse(PlanningTestFixtures.enterprisePlanSchema(), ctx);

        assertEquals("unit-test-user", plan.getCreatedBy());
    }

    @Test
    void parseShouldUseDefaultCreatedByWhenContextHasNone() {
        NetworkPlan plan = parser.parse(PlanningTestFixtures.enterprisePlanSchema(),
                PlanningTestFixtures.context());

        assertEquals("PlanningAgent", plan.getCreatedBy());
    }

    @Test
    void parseShouldNormalizeMissingOptionalListsToEmpty() {
        PlanningResponseSchema schema = PlanningResponseSchema.builder()
                .topologyNodes(null)
                .topologyLinks(null)
                .zones(null)
                .addressPlan(null)
                .vlanPlan(null)
                .securityPolicies(null)
                .planConstraints(null)
                .build();

        NetworkPlan plan = parser.parse(schema, PlanningTestFixtures.context());

        assertEquals(List.of(), plan.getTopology().getNodes());
        assertEquals(List.of(), plan.getTopology().getLinks());
        assertTrue(plan.getZones().isEmpty());
        assertTrue(plan.getAddressPlan().isEmpty());
        assertTrue(plan.getVlanPlan().isEmpty());
        assertTrue(plan.getSecurityPolicyPlan().isEmpty());
        assertTrue(plan.getPlanConstraints().isEmpty());
    }

    @Test
    void parseShouldHandleNullSchema() {
        NetworkPlan plan = parser.parse(null, PlanningTestFixtures.context());

        assertNotNull(plan.getTopology());
        assertTrue(plan.getTopology().getNodes().isEmpty());
        assertTrue(plan.getZones().isEmpty());
        assertEquals(StageStatus.SUCCESS, plan.getStageStatus());
    }
}
