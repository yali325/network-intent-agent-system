package com.yali.mactav.execution.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.safety.ExecutionSafetyPolicy;
import com.yali.mactav.model.config.CommandBlock;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.execution.ExecutionAction;
import com.yali.mactav.model.execution.ExecutionActionType;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.TestResultType;
import com.yali.mactav.model.plan.AddressPlanItem;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.NetworkZone;
import com.yali.mactav.model.plan.SecurityPolicyPlanItem;
import com.yali.mactav.model.plan.TargetEnvironment;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests structure-only conversion from plan/config DTOs into ExecutionPlan.
 */
class NetworkExecutionPlanConverterTest {

    private final NetworkExecutionPlanConverter converter = new NetworkExecutionPlanConverter();

    @Test
    void convertsPlanAndConfigToSafeExecutionPlan() {
        ExecutionPlan executionPlan = converter.convert(
                networkPlan(),
                configSet(),
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of("NETWORK_PLAN", "artifact-plan-1"));

        assertEquals("task-1", executionPlan.getTaskId());
        assertEquals("plan-1", executionPlan.getPlanId());
        assertEquals("config-set-1", executionPlan.getConfigSetId());
        assertEquals(ExecutionMode.STRUCTURE_VALIDATION, executionPlan.getExecutionMode());
        assertEquals(ExecutionEnvironmentType.STRUCTURE_VALIDATION, executionPlan.getTargetEnvironment());
        assertNotNull(executionPlan.getTopology());
        assertFalse(executionPlan.getActions().isEmpty());
        assertFalse(executionPlan.getCleanupActions().isEmpty());
        assertFalse(executionPlan.getTestCommands().isEmpty());
        assertEquals("artifact-plan-1#topology", executionPlan.getTopologyScriptRef());
        new ExecutionSafetyPolicy().validate(executionPlan);
    }

    @Test
    void conversionDoesNotCopyConfigCommandsIntoActionParameters() {
        ExecutionPlan executionPlan = converter.convert(
                networkPlan(),
                configSet(),
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of());

        for (ExecutionAction action : executionPlan.getActions()) {
            assertFalse(action.getParameters().containsKey("command"));
            assertFalse(action.getParameters().containsKey("commands"));
        }
    }

    @Test
    void derivesMininetHostsFromZonesAndAddressPlan() {
        NetworkPlan plan = networkPlan();
        plan.setTargetEnvironment(TargetEnvironment.builder()
                .adapterType("MININET_RYU")
                .simulationTarget("MININET_RYU")
                .build());
        plan.setTopology(Topology.builder()
                .nodes(List.of(TopologyNode.builder()
                        .id("rtr-edge")
                        .nodeType("ROUTER")
                        .deviceType("ROUTER")
                        .traceRefs(traceRefs())
                        .build()))
                .links(List.of())
                .build());
        plan.setZones(List.of(
                NetworkZone.builder().id("office").name("office").build(),
                NetworkZone.builder().id("server").name("server").build()));
        plan.setAddressPlan(List.of(
                AddressPlanItem.builder().id("addr-office").zoneId("office").subnet("10.10.1.0/24").traceRefs(traceRefs()).build(),
                AddressPlanItem.builder().id("addr-server").zoneId("server").subnet("10.10.2.0/24").traceRefs(traceRefs()).build()));

        ExecutionPlan executionPlan = converter.convert(
                plan,
                configSet(),
                1,
                ExecutionMode.MININET_RYU,
                ExecutionEnvironmentType.MININET_RYU,
                traceRefs(),
                Map.of("NETWORK_PLAN", "artifact-plan-1"));

        assertEquals(ExecutionMode.MININET_RYU, executionPlan.getExecutionMode());
        assertEquals(ExecutionEnvironmentType.MININET_RYU, executionPlan.getTargetEnvironment());
        assertFalse(executionPlan.getTopology().getNodes().stream()
                .filter(node -> "host".equalsIgnoreCase(node.getNodeType()))
                .toList()
                .isEmpty());
        assertFalse(executionPlan.getTopology().getLinks().isEmpty());
        assertFalse(executionPlan.getTestCommands().isEmpty());
        new ExecutionSafetyPolicy().validate(executionPlan);
    }

    @Test
    void failsMininetPlanWhenHostCannotBeDerived() {
        NetworkPlan plan = networkPlan();
        plan.setTargetEnvironment(TargetEnvironment.builder()
                .adapterType("MININET_RYU")
                .simulationTarget("MININET_RYU")
                .build());
        plan.setTopology(Topology.builder()
                .nodes(List.of(TopologyNode.builder().id("rtr-edge").nodeType("ROUTER").traceRefs(traceRefs()).build()))
                .links(List.of())
                .build());
        plan.setZones(List.of());
        plan.setAddressPlan(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> converter.convert(
                plan,
                configSet(),
                1,
                ExecutionMode.MININET_RYU,
                ExecutionEnvironmentType.MININET_RYU,
                traceRefs(),
                Map.of()));

        assertEquals("EXECUTION_PLAN_INVALID", exception.getErrorCode());
    }

    @Test
    void failsWhenPlanIdIsMissing() {
        NetworkPlan plan = networkPlan();
        plan.setPlanId(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> converter.convert(
                plan,
                configSet(),
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of()));

        assertEquals("MAC_TAV_PARAM_INVALID", exception.getErrorCode());
    }

    @Test
    void failsWhenConfigSetIdIsMissing() {
        ConfigSet configSet = configSet();
        configSet.setConfigSetId(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> converter.convert(
                networkPlan(),
                configSet,
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of()));

        assertEquals("MAC_TAV_PARAM_INVALID", exception.getErrorCode());
    }

    @Test
    void failsWhenTopologyIsMissing() {
        NetworkPlan plan = networkPlan();
        plan.setTopology(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> converter.convert(
                plan,
                configSet(),
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of()));

        assertEquals("MAC_TAV_PARAM_INVALID", exception.getErrorCode());
    }

    @Test
    void createsRelationLevelPingTestsForIntentSecurityPolicies() {
        ExecutionPlan executionPlan = converter.convert(
                relationLevelNetworkPlan(),
                configSet(),
                1,
                ExecutionMode.MININET_RYU,
                ExecutionEnvironmentType.MININET_RYU,
                traceRefs(),
                Map.of("NETWORK_PLAN", "artifact-plan-1"));

        var officeServer = testById(executionPlan, "test-ping-office-server");
        assertEquals(TestResultType.PING, officeServer.getTestType());
        assertEquals("host-zone-office", officeServer.getSourceNode());
        assertEquals("host-zone-server", officeServer.getTargetNode());
        assertEquals("reachable", officeServer.getExpectedResult());
        assertTrue(officeServer.getTraceRefs().getIntentRelationIds().contains("rel-office-server"));
        assertTrue(officeServer.getTraceRefs().getPlanElementIds().contains("policy-office-server"));
        assertTrue(officeServer.getTraceRefs().getTestIds().contains("test-ping-office-server"));

        var guestServer = testById(executionPlan, "test-ping-guest-server");
        assertEquals("host-zone-guest", guestServer.getSourceNode());
        assertEquals("host-zone-server", guestServer.getTargetNode());
        assertEquals("unreachable", guestServer.getExpectedResult());
        assertTrue(guestServer.getTraceRefs().getIntentRelationIds().contains("rel-guest-server"));
        assertTrue(guestServer.getTraceRefs().getPlanElementIds().contains("policy-guest-server"));

        var guestWeb = testById(executionPlan, "test-ping-guest-web");
        assertEquals("host-zone-guest", guestWeb.getSourceNode());
        assertEquals("host-zone-web", guestWeb.getTargetNode());
        assertEquals("reachable", guestWeb.getExpectedResult());
        assertTrue(guestWeb.getTraceRefs().getIntentRelationIds().contains("rel-guest-web"));
        assertTrue(guestWeb.getTraceRefs().getPlanElementIds().contains("policy-guest-web"));

        assertTrue(executionPlan.getTopology().getNodes().stream()
                .anyMatch(node -> "host-zone-web".equals(node.getId()) && node.getIpAddress() != null));
        new ExecutionSafetyPolicy().validate(executionPlan);
    }

    @Test
    void relationLevelExecutionPlanDoesNotContainForbiddenCommandFields() throws Exception {
        ExecutionPlan executionPlan = converter.convert(
                relationLevelNetworkPlan(),
                configSet(),
                1,
                ExecutionMode.MININET_RYU,
                ExecutionEnvironmentType.MININET_RYU,
                traceRefs(),
                Map.of("NETWORK_PLAN", "artifact-plan-1"));

        String json = new ObjectMapper().writeValueAsString(executionPlan);
        assertFalse(json.contains("\"command\""));
        assertFalse(json.contains("\"cmd\""));
        assertFalse(json.contains("\"shell\""));
        assertFalse(json.contains("\"script\""));
        assertFalse(json.contains("\"cli\""));
        assertFalse(json.contains("\"rawCommand\""));
    }

    @Test
    void failsWhenRelationLevelTargetHostCannotBeDerived() {
        NetworkPlan plan = relationLevelNetworkPlan();
        plan.setAddressPlan(List.of(
                AddressPlanItem.builder().id("addr-office").zoneId("zone-office").subnet("10.10.1.0/24").traceRefs(traceRefs()).build(),
                AddressPlanItem.builder().id("addr-server").zoneId("zone-server").subnet("10.10.2.0/24").traceRefs(traceRefs()).build(),
                AddressPlanItem.builder().id("addr-guest").zoneId("zone-guest").subnet("10.10.3.0/24").traceRefs(traceRefs()).build()));

        BusinessException exception = assertThrows(BusinessException.class, () -> converter.convert(
                plan,
                configSet(),
                1,
                ExecutionMode.MININET_RYU,
                ExecutionEnvironmentType.MININET_RYU,
                traceRefs(),
                Map.of("NETWORK_PLAN", "artifact-plan-1")));

        assertEquals("EXECUTION_PLAN_INVALID", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("policy-guest-web"));
    }

    @Test
    void skipsRelationLevelPolicyWithoutZonesWithoutBlockingExecution() {
        NetworkPlan plan = relationLevelNetworkPlan();
        plan.setSecurityPolicyPlan(List.of(
                securityPolicy("policy-missing-target", "zone-guest", null, "DENY", "rel-guest-server")));

        ExecutionPlan executionPlan = converter.convert(
                plan,
                configSet(),
                1,
                ExecutionMode.MININET_RYU,
                ExecutionEnvironmentType.MININET_RYU,
                traceRefs(),
                Map.of("NETWORK_PLAN", "artifact-plan-1"));

        assertFalse(executionPlan.getTestCommands().stream()
                .anyMatch(test -> "test-ping-guest-server".equals(test.getTestId())));
        assertFalse(executionPlan.getTestCommands().isEmpty());
    }

    @Test
    void structureValidationDoesNotRequireDerivedRelationHosts() {
        NetworkPlan plan = relationLevelNetworkPlan();
        plan.setTargetEnvironment(TargetEnvironment.builder()
                .adapterType("STRUCTURE_VALIDATION")
                .simulationTarget("STRUCTURE_VALIDATION")
                .build());

        ExecutionPlan executionPlan = converter.convert(
                plan,
                configSet(),
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of("NETWORK_PLAN", "artifact-plan-1"));

        assertEquals(ExecutionMode.STRUCTURE_VALIDATION, executionPlan.getExecutionMode());
        assertFalse(executionPlan.getTestCommands().stream()
                .anyMatch(test -> "test-ping-office-guest-isolation".equals(test.getTestId())));
        assertFalse(executionPlan.getTestCommands().isEmpty());
    }

    private NetworkPlan networkPlan() {
        return NetworkPlan.builder()
                .planId("plan-1")
                .taskId("task-1")
                .targetEnvironment(TargetEnvironment.builder()
                        .adapterType("STRUCTURE_VALIDATION")
                        .simulationTarget("STRUCTURE_VALIDATION")
                        .build())
                .topology(Topology.builder()
                        .nodes(List.of(
                                TopologyNode.builder().id("host-office").nodeType("HOST").traceRefs(traceRefs()).build(),
                                TopologyNode.builder().id("host-server").nodeType("HOST").traceRefs(traceRefs()).build(),
                                TopologyNode.builder().id("switch-1").nodeType("SWITCH").traceRefs(traceRefs()).build()))
                        .links(List.of(TopologyLink.builder()
                                .id("link-1")
                                .sourceNode("host-office")
                                .targetNode("switch-1")
                                .traceRefs(traceRefs())
                                .build()))
                        .build())
                .traceRefs(traceRefs())
                .build();
    }

    private NetworkPlan relationLevelNetworkPlan() {
        return NetworkPlan.builder()
                .planId("plan-1")
                .taskId("task-1")
                .targetEnvironment(TargetEnvironment.builder()
                        .adapterType("MININET_RYU")
                        .simulationTarget("MININET_RYU")
                        .build())
                .topology(Topology.builder()
                        .nodes(List.of(TopologyNode.builder()
                                .id("sw-core")
                                .nodeType("SWITCH")
                                .traceRefs(traceRefs())
                                .build()))
                        .links(List.of(TopologyLink.builder()
                                .id("link-core-loopback")
                                .sourceNode("sw-core")
                                .targetNode("sw-core")
                                .traceRefs(traceRefs())
                                .build()))
                        .build())
                .zones(List.of(
                        NetworkZone.builder().id("zone-office").name("office").build(),
                        NetworkZone.builder().id("zone-server").name("server").build(),
                        NetworkZone.builder().id("zone-guest").name("guest").build(),
                        NetworkZone.builder().id("zone-web").name("web").build()))
                .addressPlan(List.of(
                        AddressPlanItem.builder().id("addr-office").zoneId("zone-office").subnet("10.10.1.0/24").traceRefs(traceRefs()).build(),
                        AddressPlanItem.builder().id("addr-server").zoneId("zone-server").subnet("10.10.2.0/24").traceRefs(traceRefs()).build(),
                        AddressPlanItem.builder().id("addr-guest").zoneId("zone-guest").subnet("10.10.3.0/24").traceRefs(traceRefs()).build(),
                        AddressPlanItem.builder().id("addr-web").zoneId("zone-web").subnet("10.10.4.0/24").traceRefs(traceRefs()).build()))
                .securityPolicyPlan(List.of(
                        securityPolicy("policy-office-server", "zone-office", "zone-server", "ALLOW", "rel-office-server"),
                        securityPolicy("policy-guest-server", "zone-guest", "zone-server", "DENY", "rel-guest-server"),
                        securityPolicy("policy-guest-web", "zone-guest", "zone-web", "ALLOW", "rel-guest-web")))
                .traceRefs(traceRefs())
                .build();
    }

    private SecurityPolicyPlanItem securityPolicy(
            String policyId,
            String sourceZone,
            String targetZone,
            String action,
            String intentRelationId) {
        return SecurityPolicyPlanItem.builder()
                .id(policyId)
                .name(policyId)
                .sourceZone(sourceZone)
                .targetZone(targetZone)
                .action(action)
                .service("IP")
                .basedOnIntentRelation(intentRelationId)
                .traceRefs(TraceRefs.builder()
                        .intentRelationIds(List.of(intentRelationId))
                        .planElementIds(List.of(policyId))
                        .build())
                .build();
    }

    private com.yali.mactav.model.execution.TestCommand testById(ExecutionPlan executionPlan, String testId) {
        return executionPlan.getTestCommands().stream()
                .filter(test -> testId.equals(test.getTestId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing test " + testId));
    }

    private ConfigSet configSet() {
        return ConfigSet.builder()
                .configSetId("config-set-1")
                .planId("plan-1")
                .taskId("task-1")
                .deviceConfigs(List.of(DeviceConfig.builder()
                        .deviceId("switch-1")
                        .deviceName("switch-1")
                        .commandBlocks(List.of(CommandBlock.builder()
                                .blockId("config-block-1")
                                .commands(List.of("system-view", "acl 3000"))
                                .traceRefs(traceRefs())
                                .build()))
                        .traceRefs(traceRefs())
                        .build()))
                .traceRefs(traceRefs())
                .build();
    }

    private TraceRefs traceRefs() {
        return TraceRefs.builder()
                .intentRelationIds(List.of("intent-relation-1"))
                .planElementIds(List.of("plan-node-1"))
                .configBlockIds(List.of("config-block-1"))
                .testIds(List.of("test-" + TestResultType.PING.name().toLowerCase()))
                .build();
    }
}
