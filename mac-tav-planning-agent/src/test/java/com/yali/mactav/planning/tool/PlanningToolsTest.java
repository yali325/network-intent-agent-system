package com.yali.mactav.planning.tool;

import static com.yali.mactav.planning.PlanningTestFixtures.SAMPLE_INTENT_JSON;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Offline tool tests for PlanningAgent planning tools.
 *
 * <p>Tests verify tool outputs for known inputs and do not call real models.</p>
 */
class PlanningToolsTest {

    private final AddressPlanningTool addressTool = new AddressPlanningTool();
    private final VlanPlanningTool vlanTool = new VlanPlanningTool();
    private final TopologyTemplateTool topoTool = new TopologyTemplateTool();
    private final PlanningPlaybookTool playbookTool = new PlanningPlaybookTool();

    @Test
    void addressToolShouldSuggestForOfficeGuestServer() {
        var result = addressTool.suggestAddressPlan(SAMPLE_INTENT_JSON, "lab");

        assertNotNull(result);
        assertEquals(3, result.suggestions().size());
        assertTrue(result.suggestions().stream().anyMatch(s -> s.zoneId().equals("zone-office")));
        assertTrue(result.suggestions().stream().anyMatch(s -> s.zoneId().equals("zone-guest")));
        assertTrue(result.suggestions().stream().anyMatch(s -> s.zoneId().equals("zone-server")));
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void addressToolShouldReturnWarningForEmptyIntent() {
        var result = addressTool.suggestAddressPlan("", null);

        assertNotNull(result);
        assertTrue(result.suggestions().isEmpty());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void vlanToolShouldSuggestForOfficeGuestServer() {
        var result = vlanTool.suggestVlanPlan(SAMPLE_INTENT_JSON, "lab");

        assertNotNull(result);
        assertEquals(3, result.suggestions().size());
        assertTrue(result.suggestions().stream().anyMatch(s -> s.zoneId().equals("zone-office")));
        assertEquals(100, result.suggestions().get(0).vlanId());
    }

    @Test
    void topoToolShouldSuggestForOfficeGuestServer() {
        var result = topoTool.suggestTopology(SAMPLE_INTENT_JSON);

        assertNotNull(result);
        assertTrue(result.nodes().size() >= 4);
        assertTrue(result.links().size() >= 3);
        assertTrue(result.nodes().stream().anyMatch(n -> n.id().equals("sw-core")));
    }

    @Test
    void playbookToolShouldSuggestForOfficeGuestServer() {
        var result = playbookTool.suggestPlaybook(SAMPLE_INTENT_JSON);

        assertNotNull(result);
        assertTrue(result.securityPolicies().size() >= 2);
        assertTrue(result.securityPolicies().stream()
                .anyMatch(sp -> sp.action().equals("ALLOW")));
        assertTrue(result.securityPolicies().stream()
                .anyMatch(sp -> sp.action().equals("DENY")));
        assertEquals(1, result.routingHints().size());
        assertEquals("OSPF", result.routingHints().get(0).protocol());
        assertTrue(result.routingHints().get(0).routerCandidates().stream()
                .anyMatch(router -> router.deviceId().equals("rtr-edge")));
        assertTrue(result.routingHints().get(0).traceIntentNodeIds().contains("node-office"));
    }
}
