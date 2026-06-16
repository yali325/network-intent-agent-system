package com.yali.mactav.configuration.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.agent.core.validator.AgentValidationException;
import com.yali.mactav.configuration.ConfigurationTestFixtures;
import com.yali.mactav.configuration.parser.ConfigurationResponseParser;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.configuration.service.ConfigurationTraceRefsStabilizer;
import com.yali.mactav.configuration.validator.ConfigurationOutputValidator;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import com.yali.mactav.model.plan.AddressPlanItem;
import com.yali.mactav.model.plan.EnforcementPoint;
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
import java.util.ArrayList;
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
            new ConfigurationConfigSetNormalizer(),
            new DeterministicPolicyConfigBuilder(objectMapper),
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
    void parseShouldSanitizeCliTextFromGenerationSummary() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        schema.setGenerationSummary("""
                system-view
                acl number 3001
                rule deny ip source 10.1.0.0 0.0.0.255 destination 10.2.0.0 0.0.0.255
                interface GigabitEthernet0/0/1
                port trunk allow-pass vlan 100
                return
                """);

        ConfigSet configSet = service.parse(schema, payload());

        assertTrue(configSet.getGenerationSummary().startsWith("Generated configuration set for task "));
        assertFalse(configSet.getGenerationSummary().contains("system-view"));
        assertFalse(configSet.getGenerationSummary().contains("acl number"));
        new ConfigurationOutputValidator().validateAndReturn(configSet);
    }

    @Test
    void parseShouldPreserveNormalShortGenerationSummary() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        schema.setGenerationSummary("Generated ACL and VLAN configuration blocks from the approved NetworkPlan.");

        ConfigSet configSet = service.parse(schema, payload());

        assertEquals("Generated ACL and VLAN configuration blocks from the approved NetworkPlan.",
                configSet.getGenerationSummary());
    }

    @Test
    void parseShouldCreateSummaryWhenGenerationSummaryIsBlank() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        schema.setGenerationSummary("  ");

        ConfigSet configSet = service.parse(schema, payload());

        assertEquals("Generated configuration set for task " + ConfigurationTestFixtures.TASK_ID
                + ", devices=2, commandBlocks=3.", configSet.getGenerationSummary());
    }

    @Test
    void parseShouldNotHideEmptyCommandBlocksFailureWhenSummaryIsSanitized() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        schema.setGenerationSummary("interface GigabitEthernet0/0/1\n port link-type trunk");
        schema.getDeviceConfigs().get(0).setCommandBlocks(List.of());

        AgentValidationException error = assertThrows(AgentValidationException.class,
                () -> service.parse(schema, payload(planWithoutTraceablePolicy())));

        assertTrue(error.getMessage().contains("commandBlocks"));
    }

    @Test
    void parseShouldSerializeSanitizedConfigSetAndPassValidator() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        schema.setGenerationSummary("display current-configuration\ninterface GE0/0/1");

        ConfigSet configSet = service.parse(schema, payload());
        String payloadJson = mapper.writeValueAsString(configSet);
        ConfigSet reparsed = mapper.readValue(payloadJson, ConfigSet.class);

        assertFalse(reparsed.getGenerationSummary().contains("display current-configuration"));
        new ConfigurationOutputValidator().validateAndReturn(reparsed);
    }

    @Test
    void parseShouldThrowWhenValidatorFails() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        schema.setDeviceConfigs(null);

        assertThrows(AgentValidationException.class, () -> service.parse(schema, payload(planWithoutTraceablePolicy())));
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

    @Test
    void parseShouldGenerateDeterministicPolicyCommandBlocksFromSecurityPolicyPlan() {
        ConfigurationResponseSchema schema = vlanOnlySchema();

        ConfigSet configSet = service.parse(schema, payload(policyRichNetworkPlan()));
        String commands = commandText(configSet);

        assertTrue(commands.contains("rule deny ip source 10.10.0.0 0.0.0.255 destination 10.20.0.0 0.0.0.255"));
        assertTrue(commands.contains("rule deny ip source 10.20.0.0 0.0.0.255 destination 10.30.0.0 0.0.0.255"));
        assertTrue(commands.contains("rule permit ip source 10.10.0.0 0.0.0.255 destination 10.30.0.0 0.0.0.255"));
        assertTrue(commandBlockIds(configSet).contains("cb-policy-policy-office-guest"));
        assertTrue(commandBlockIds(configSet).contains("cb-policy-policy-guest-server"));
        assertTrue(commandBlockIds(configSet).contains("cb-policy-policy-office-server"));
        new ConfigurationOutputValidator().validateAndReturn(configSet);
    }

    @Test
    void parseShouldNotDuplicateExistingLegalPolicyAclBlock() {
        ConfigurationResponseSchema schema = vlanOnlySchema();
        schema.getDeviceConfigs().get(0).setCommandBlocks(new ArrayList<>(schema.getDeviceConfigs().get(0).getCommandBlocks()));
        schema.getDeviceConfigs().get(0).getCommandBlocks().add(ConfigurationResponseSchema.CommandBlockSchema.builder()
                .blockId("existing-office-server-acl")
                .blockType("ACL_POLICY")
                .title("Existing office server allow ACL")
                .commands(List.of(
                        "acl number 3010",
                        "rule permit ip source 10.10.0.0 0.0.0.255 destination 10.30.0.0 0.0.0.255",
                        "return"))
                .rollbackCommands(List.of("undo acl number 3010"))
                .explanation("Existing policy-office-server ACL.")
                .traceRefs(ConfigurationResponseSchema.TraceRefsSchema.builder()
                        .planElementIds(List.of("policy-office-server"))
                        .intentRelationIds(List.of("rel-office-server"))
                        .build())
                .build());

        ConfigSet configSet = service.parse(schema, payload(policyRichNetworkPlan()));

        long officeServerBlocks = configSet.getDeviceConfigs().stream()
                .flatMap(device -> device.getCommandBlocks().stream())
                .filter(block -> block.getTraceRefs() != null
                        && block.getTraceRefs().getPlanElementIds().contains("policy-office-server"))
                .count();
        assertEquals(1, officeServerBlocks);
    }

    @Test
    void parseShouldFailWhenPolicySubnetCannotBeResolved() {
        ConfigurationResponseSchema schema = vlanOnlySchema();
        NetworkPlan plan = policyRichNetworkPlan();
        plan.setAddressPlan(List.of(AddressPlanItem.builder()
                .id("addr-office")
                .zoneId("office")
                .subnet("10.10.0.0/24")
                .build()));

        BusinessException error = assertThrows(BusinessException.class, () -> service.parse(schema, payload(plan)));

        assertTrue(error.getMessage().contains("missing targetZone subnet")
                || error.getMessage().contains("missing sourceZone subnet"));
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
                .addressPlan(List.of(
                        AddressPlanItem.builder()
                                .id("addr-office")
                                .zoneId("office")
                                .subnet("10.1.0.0/24")
                                .build(),
                        AddressPlanItem.builder()
                                .id("addr-server")
                                .zoneId("server")
                                .subnet("10.2.0.0/24")
                                .build()))
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

    private NetworkPlan policyRichNetworkPlan() {
        NetworkPlan plan = networkPlan();
        plan.setAddressPlan(List.of(
                AddressPlanItem.builder().id("addr-office").zoneId("office").subnet("10.10.0.0/24").build(),
                AddressPlanItem.builder().id("addr-guest").zoneId("guest").subnet("10.20.0.0/24").build(),
                AddressPlanItem.builder().id("addr-server").zoneId("server").subnet("10.30.0.0/24").build()));
        plan.setSecurityPolicyPlan(List.of(
                SecurityPolicyPlanItem.builder()
                        .id("policy-office-guest")
                        .name("office guest isolation")
                        .sourceZone("office")
                        .targetZone("guest")
                        .action("DENY")
                        .service("ALL")
                        .enforcementPoint(EnforcementPoint.builder()
                                .deviceId("sw-core")
                                .interfaceName("GigabitEthernet0/0/1")
                                .direction("inbound")
                                .build())
                        .basedOnIntentRelation("rel-office-guest")
                        .traceRefs(TraceRefs.builder()
                                .planElementIds(List.of("policy-office-guest"))
                                .intentRelationIds(List.of("rel-office-guest"))
                                .build())
                        .build(),
                SecurityPolicyPlanItem.builder()
                        .id("policy-guest-server")
                        .name("guest server deny")
                        .sourceZone("guest")
                        .targetZone("server")
                        .action("DENY")
                        .service("ALL")
                        .basedOnIntentRelation("rel-guest-server")
                        .traceRefs(TraceRefs.builder()
                                .planElementIds(List.of("policy-guest-server"))
                                .intentRelationIds(List.of("rel-guest-server"))
                                .build())
                        .build(),
                SecurityPolicyPlanItem.builder()
                        .id("policy-office-server")
                        .name("office server allow")
                        .sourceZone("office")
                        .targetZone("server")
                        .action("ALLOW")
                        .service("ALL")
                        .basedOnIntentRelation("rel-office-server")
                        .traceRefs(TraceRefs.builder()
                                .planElementIds(List.of("policy-office-server"))
                                .intentRelationIds(List.of("rel-office-server"))
                                .build())
                        .build()));
        return plan;
    }

    private ConfigurationResponseSchema vlanOnlySchema() {
        ConfigurationResponseSchema schema = ConfigurationTestFixtures.sampleSchema(objectMapper);
        var device = schema.getDeviceConfigs().get(0);
        device.setDeviceId("sw-core");
        device.setCommandBlocks(List.of(ConfigurationResponseSchema.CommandBlockSchema.builder()
                .blockId("vlan-only-office")
                .blockType("VLAN")
                .title("Create office VLAN")
                .commands(List.of("vlan 10", "name Office_VLAN"))
                .rollbackCommands(List.of("undo vlan 10"))
                .explanation("Auxiliary VLAN block only.")
                .traceRefs(ConfigurationResponseSchema.TraceRefsSchema.builder()
                        .planElementIds(List.of("vlan-office"))
                        .build())
                .build()));
        schema.setDeviceConfigs(List.of(device));
        return schema;
    }

    private String commandText(ConfigSet configSet) {
        StringBuilder builder = new StringBuilder();
        configSet.getDeviceConfigs().forEach(device -> device.getCommandBlocks()
                .forEach(block -> builder.append(String.join("\n", block.getCommands())).append('\n')));
        return builder.toString();
    }

    private List<String> commandBlockIds(ConfigSet configSet) {
        return configSet.getDeviceConfigs().stream()
                .flatMap(device -> device.getCommandBlocks().stream())
                .map(block -> block.getBlockId())
                .toList();
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
