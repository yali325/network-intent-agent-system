package com.yali.mactav.configuration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.validator.AgentValidationException;
import com.yali.mactav.configuration.ConfigurationTestFixtures;
import com.yali.mactav.configuration.parser.ConfigurationResponseParser;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.configuration.service.ConfigurationTraceRefsStabilizer;
import com.yali.mactav.configuration.validator.ConfigurationOutputValidator;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.PortRef;
import com.yali.mactav.model.plan.RoutingPlan;
import com.yali.mactav.model.plan.RoutingRouter;
import com.yali.mactav.model.plan.SecurityPolicyPlanItem;
import com.yali.mactav.model.plan.TargetEnvironment;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.plan.VlanPlanItem;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline tests for ConfigurationService parser and validator orchestration.
 */
class ConfigurationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConfigurationService service = new ConfigurationServiceImpl(
            new ConfigurationResponseParser(),
            new ConfigurationOutputValidator(),
            new ConfigurationTraceRefsStabilizer(objectMapper));

    @Test
    void parseShouldReturnValidatedConfigSet() {
        ConfigSet configSet = service.parse(ConfigurationTestFixtures.sampleSchema(objectMapper), payload());

        assertEquals(ConfigurationTestFixtures.TASK_ID, configSet.getTaskId());
        assertEquals(1, configSet.getPlanVersion());
        assertEquals(1, configSet.getConfigVersion());
        assertFalse(configSet.getDeviceConfigs().isEmpty());
        assertFalse(configSet.getGenerationSources().isEmpty());
    }

    @Test
    void parseShouldThrowWhenValidatorFails() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        schema.setDeviceConfigs(null);

        assertThrows(AgentValidationException.class, () -> service.parse(schema, payload()));
    }

    @Test
    void parseShouldDeriveMissingCommandBlockTraceRefsFromMatchingPolicy() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        var block = schema.getDeviceConfigs().get(0).getCommandBlocks().get(0);
        block.setTraceRefs(null);
        block.setExplanation("Generate ACL for policy-office-server deny office server access.");

        ConfigSet configSet = service.parse(schema, payload());
        var refs = configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).getTraceRefs();

        assertTrue(refs.getPlanElementIds().contains("policy-office-server"));
        assertTrue(refs.getIntentRelationIds().contains("rel-office-server"));
        new ConfigurationOutputValidator().validateAndReturn(configSet);
    }

    @Test
    void parseShouldPreserveExistingValidCommandBlockTraceRefs() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        var block = schema.getDeviceConfigs().get(0).getCommandBlocks().get(0);
        block.setTraceRefs(ConfigurationResponseSchema.TraceRefsSchema.builder()
                .planElementIds(List.of("policy-office-server"))
                .intentRelationIds(List.of("rel-office-server"))
                .build());

        ConfigSet configSet = service.parse(schema, payload());
        var refs = configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).getTraceRefs();

        assertEquals(List.of("policy-office-server"), refs.getPlanElementIds());
        assertEquals(List.of("rel-office-server"), refs.getIntentRelationIds());
    }

    @Test
    void parseShouldCorrectInvalidCommandBlockTraceRefsWhenPolicyMatches() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        var block = schema.getDeviceConfigs().get(0).getCommandBlocks().get(0);
        block.setTraceRefs(ConfigurationResponseSchema.TraceRefsSchema.builder()
                .planElementIds(List.of("missing-plan-id"))
                .intentRelationIds(List.of("missing-relation-id"))
                .build());
        block.setExplanation("Apply policy-office-server for office to server deny.");

        ConfigSet configSet = service.parse(schema, payload());
        var refs = configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).getTraceRefs();

        assertFalse(refs.getPlanElementIds().contains("missing-plan-id"));
        assertFalse(refs.getIntentRelationIds().contains("missing-relation-id"));
        assertTrue(refs.getPlanElementIds().contains("policy-office-server"));
        assertTrue(refs.getIntentRelationIds().contains("rel-office-server"));
    }

    @Test
    void parseShouldDeriveVlanTraceRefsFromVlanPlanId() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        var device = schema.getDeviceConfigs().get(0);
        device.setDeviceId("sw-core");
        var block = device.getCommandBlocks().get(0);
        block.setTraceRefs(null);
        block.setBlockType("VLAN");
        block.setTitle("Create VLAN 100");
        block.setCommands(List.of("vlan 100"));

        ConfigSet configSet = service.parse(schema, payload(networkPlan()));
        var refs = configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).getTraceRefs();

        assertTrue(refs.getPlanElementIds().contains("vlan-office"));
        new ConfigurationOutputValidator().validateAndReturn(configSet);
    }

    @Test
    void parseShouldDeriveInterfaceTraceRefsFromTopologyLinkOrTrunkPort() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        var device = schema.getDeviceConfigs().get(0);
        device.setDeviceId("sw-core");
        var block = device.getCommandBlocks().get(0);
        block.setTraceRefs(null);
        block.setBlockType("INTERFACE");
        block.setTitle("Configure trunk interface");
        block.setCommands(List.of("interface GigabitEthernet0/0/1", "port link-type trunk"));

        ConfigSet configSet = service.parse(schema, payload(networkPlan()));
        var refs = configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).getTraceRefs();

        assertTrue(refs.getPlanElementIds().contains("link-core-office")
                || refs.getPlanElementIds().contains("vlan-office"));
        new ConfigurationOutputValidator().validateAndReturn(configSet);
    }

    @Test
    void parseShouldDeriveRoutingTraceRefsFromRoutingPlanAndRouter() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        var device = schema.getDeviceConfigs().get(0);
        device.setDeviceId("rtr-edge");
        var block = device.getCommandBlocks().get(0);
        block.setTraceRefs(null);
        block.setBlockType("ROUTING");
        block.setTitle("Configure default route");
        block.setCommands(List.of("ip route-static 0.0.0.0 0.0.0.0 10.0.0.1"));

        ConfigSet configSet = service.parse(schema, payload(networkPlan()));
        var refs = configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).getTraceRefs();

        assertTrue(refs.getPlanElementIds().contains("routing-ospf"));
        assertTrue(refs.getPlanElementIds().contains("router-edge"));
        new ConfigurationOutputValidator().validateAndReturn(configSet);
    }

    @Test
    void parseShouldDeriveDeviceTraceRefsFromTopologyNodeId() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        var device = schema.getDeviceConfigs().get(0);
        device.setDeviceId("sw-office");
        var block = device.getCommandBlocks().get(0);
        block.setTraceRefs(null);
        block.setBlockType("DEVICE");
        block.setTitle("Device base configuration");
        block.setExplanation("Apply device base configuration to sw-office.");

        ConfigSet configSet = service.parse(schema, payload(networkPlan()));
        var refs = configSet.getDeviceConfigs().get(0).getCommandBlocks().get(0).getTraceRefs();

        assertTrue(refs.getPlanElementIds().contains("sw-office"));
        new ConfigurationOutputValidator().validateAndReturn(configSet);
    }

    @Test
    void parseShouldStillFailWhenTraceRefsCannotBeDerived() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        var block = schema.getDeviceConfigs().get(0).getCommandBlocks().get(0);
        block.setTraceRefs(null);
        block.setExplanation("Generic configuration block with no unique policy reference.");

        ConfigurationAgentInvokePayload payload = payload(planWithoutTraceablePolicy());

        assertThrows(AgentValidationException.class, () -> service.parse(schema, payload));
    }

    private ConfigurationAgentInvokePayload payload() {
        return payload(networkPlan());
    }

    private ConfigurationAgentInvokePayload payload(NetworkPlan networkPlan) {
        return ConfigurationAgentInvokePayload.builder()
                .taskId(ConfigurationTestFixtures.TASK_ID)
                .rawText("Generate configuration")
                .planVersion(1)
                .planJson(writePlanJson(networkPlan))
                .configVersion(1)
                .traceId(ConfigurationTestFixtures.TRACE_ID)
                .workspaceSnapshot("{}")
                .createdBy("unit-test")
                .build();
    }

    private String writePlanJson(NetworkPlan networkPlan) {
        try {
            return objectMapper.writeValueAsString(networkPlan);
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private NetworkPlan networkPlan() {
        return NetworkPlan.builder()
                .taskId(ConfigurationTestFixtures.TASK_ID)
                .planId("plan-1")
                .planVersion(1)
                .targetEnvironment(TargetEnvironment.builder().vendor("generic").configStyle("acl").build())
                .topology(Topology.builder()
                        .nodes(List.of(
                                TopologyNode.builder()
                                        .id("sw-core")
                                        .name("core-switch")
                                        .nodeType("SWITCH")
                                        .traceRefs(TraceRefs.builder()
                                                .planElementIds(List.of("topology-sw-core"))
                                                .intentRelationIds(List.of("rel-office-server"))
                                                .build())
                                        .build(),
                                TopologyNode.builder()
                                        .id("sw-office")
                                        .name("office-switch")
                                        .nodeType("SWITCH")
                                        .traceRefs(TraceRefs.builder()
                                                .planElementIds(List.of("topology-sw-office"))
                                                .intentRelationIds(List.of("rel-office-server"))
                                                .build())
                                        .build(),
                                TopologyNode.builder()
                                        .id("rtr-edge")
                                        .name("edge-router")
                                        .nodeType("ROUTER")
                                        .build()))
                        .links(List.of(TopologyLink.builder()
                                .id("link-core-office")
                                .sourceNode("sw-core")
                                .sourceInterface("GigabitEthernet0/0/1")
                                .targetNode("sw-office")
                                .targetInterface("GigabitEthernet0/0/24")
                                .build()))
                        .build())
                .vlanPlan(List.of(VlanPlanItem.builder()
                        .id("vlan-office")
                        .vlanId(100)
                        .name("office")
                        .zoneId("zone-office")
                        .trunkPorts(List.of(PortRef.builder()
                                .deviceId("sw-core")
                                .interfaceName("GigabitEthernet0/0/1")
                                .build()))
                        .build()))
                .routingPlan(RoutingPlan.builder()
                        .id("routing-ospf")
                        .protocol("OSPF")
                        .routers(List.of(RoutingRouter.builder()
                                .id("router-edge")
                                .deviceId("rtr-edge")
                                .routerId("1.1.1.1")
                                .build()))
                        .build())
                .securityPolicyPlan(List.of(SecurityPolicyPlanItem.builder()
                        .id("policy-office-server")
                        .name("office server deny")
                        .sourceZone("office")
                        .targetZone("server")
                        .action("DENY")
                        .service("any")
                        .basedOnIntentRelation("rel-office-server")
                        .traceRefs(TraceRefs.builder()
                                .planElementIds(List.of("policy-office-server"))
                                .intentRelationIds(List.of("rel-office-server"))
                                .build())
                        .build()))
                .traceRefs(TraceRefs.builder()
                        .planElementIds(List.of("plan-1"))
                        .intentRelationIds(List.of("rel-office-server"))
                        .build())
                .build();
    }

    private NetworkPlan planWithoutTraceablePolicy() {
        return NetworkPlan.builder()
                .taskId(ConfigurationTestFixtures.TASK_ID)
                .planId("plan-1")
                .planVersion(1)
                .targetEnvironment(TargetEnvironment.builder().vendor("generic").configStyle("acl").build())
                .securityPolicyPlan(List.of())
                .traceRefs(TraceRefs.builder().planElementIds(List.of("plan-1")).build())
                .build();
    }
}
