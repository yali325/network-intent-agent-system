package com.yali.mactav.planning;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.plan.*;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.planning.parser.PlanningResponseParser;
import com.yali.mactav.planning.request.PlanningAgentRequest;
import com.yali.mactav.planning.schema.PlanningResponseSchema;
import com.yali.mactav.planning.schema.PlanningResponseSchema.*;
import java.util.List;

/**
 * Fixed offline fixtures for PlanningAgent parser, validator, and service tests.
 *
 * <p>The fixture represents docs/07 enterprise-office-guest-success style data
 * without creating fake agents, mock tools, or model-call substitutes.</p>
 */
public final class PlanningTestFixtures {

    public static final String TASK_ID = "task-enterprise-office-guest";

    public static final String TRACE_ID = "trace-enterprise-office-guest";

    public static final String RAW_TEXT = "Build an office and guest network policy where office can access "
            + "server, guest cannot access server, office and guest are isolated, and OSPF is preferred.";

    public static final String SAMPLE_INTENT_JSON = "{\"taskId\":\"task-enterprise-office-guest\","
            + "\"nodes\":[{\"id\":\"node-office\",\"name\":\"office\"},"
            + "{\"id\":\"node-guest\",\"name\":\"guest\"},"
            + "{\"id\":\"node-server\",\"name\":\"server\"}],"
            + "\"relations\":[{\"id\":\"rel-office-server\",\"source\":\"node-office\","
            + "\"target\":\"node-server\",\"action\":\"ALLOW\"},"
            + "{\"id\":\"rel-guest-server\",\"source\":\"node-guest\","
            + "\"target\":\"node-server\",\"action\":\"DENY\"},"
            + "{\"id\":\"rel-office-guest\",\"source\":\"node-office\","
            + "\"target\":\"node-guest\",\"action\":\"DENY\"}],"
            + "\"preferences\":[{\"id\":\"pref-routing-protocol\",\"value\":\"OSPF\"}]}";

    private PlanningTestFixtures() {
    }

    public static PlanningAgentRequest request() {
        return PlanningAgentRequest.builder()
                .taskId(TASK_ID)
                .rawText(RAW_TEXT)
                .intentVersion(2)
                .intentJson(SAMPLE_INTENT_JSON)
                .planVersion(1)
                .traceId(TRACE_ID)
                .userContext("enterprise office")
                .workspaceSnapshot("{}")
                .targetEnvironmentHint("lab")
                .createdBy("unit-test")
                .build();
    }

    public static AgentRunContext context() {
        return AgentRunContext.builder()
                .taskId(TASK_ID)
                .stage(WorkflowStage.PLANNING)
                .version(1)
                .traceId(TRACE_ID)
                .userInput(RAW_TEXT)
                .workspaceSnapshot("{}")
                .build();
    }

    public static PlanningResponseSchema enterprisePlanSchema() {
        return PlanningResponseSchema.builder()
                .planSummary("Three-zone enterprise network with office, guest, and server zones. "
                        + "Office can access server; guest cannot access server; office and guest are isolated. "
                        + "OSPF routing with private addressing.")
                .selectedArchitecture(ArchitectureSchema.builder()
                        .id("arch-core-distribution")
                        .type("TIERED_LAN")
                        .reason("Supports zone-based isolation with centralized policy enforcement")
                        .tradeoffs(List.of("Higher switch cost", "Single point of policy control"))
                        .build())
                .targetEnvironment(TargetEnvironmentSchema.builder()
                        .vendor("generic")
                        .configStyle("structured")
                        .simulationTarget("mininet")
                        .adapterType("structured-validation")
                        .build())
                .topologyNodes(List.of(
                        TopologyNodeSchema.builder().id("sw-core").name("core-switch")
                                .nodeType("SWITCH").deviceType("SWITCH_L3").role("core")
                                .vendor("generic").zoneId("zone-core")
                                .traceIntentNodeIds(List.of("node-office", "node-guest", "node-server")).build(),
                        TopologyNodeSchema.builder().id("rtr-edge").name("edge-router")
                                .nodeType("ROUTER").deviceType("ROUTER").role("gateway")
                                .vendor("generic").zoneId("zone-core").build(),
                        TopologyNodeSchema.builder().id("sw-office").name("office-switch")
                                .nodeType("SWITCH").deviceType("SWITCH_L2").role("access")
                                .vendor("generic").zoneId("zone-office")
                                .traceIntentNodeIds(List.of("node-office")).build(),
                        TopologyNodeSchema.builder().id("sw-guest").name("guest-switch")
                                .nodeType("SWITCH").deviceType("SWITCH_L2").role("access")
                                .vendor("generic").zoneId("zone-guest")
                                .traceIntentNodeIds(List.of("node-guest")).build(),
                        TopologyNodeSchema.builder().id("sw-server").name("server-switch")
                                .nodeType("SWITCH").deviceType("SWITCH_L2").role("access")
                                .vendor("generic").zoneId("zone-server")
                                .traceIntentNodeIds(List.of("node-server")).build()
                ))
                .topologyLinks(List.of(
                        TopologyLinkSchema.builder().id("link-core-office").sourceNode("sw-core")
                                .targetNode("sw-office").sourceInterface("Gi0/1")
                                .targetInterface("Gi0/1").linkType("TRUNK").build(),
                        TopologyLinkSchema.builder().id("link-core-guest").sourceNode("sw-core")
                                .targetNode("sw-guest").sourceInterface("Gi0/2")
                                .targetInterface("Gi0/1").linkType("TRUNK").build(),
                        TopologyLinkSchema.builder().id("link-core-server").sourceNode("sw-core")
                                .targetNode("sw-server").sourceInterface("Gi0/3")
                                .targetInterface("Gi0/1").linkType("TRUNK").build(),
                        TopologyLinkSchema.builder().id("link-core-edge").sourceNode("sw-core")
                                .targetNode("rtr-edge").sourceInterface("Gi0/4")
                                .targetInterface("Gi0/1").linkType("ROUTED").build()
                ))
                .zones(List.of(
                        ZoneSchema.builder().id("zone-office").name("office")
                                .mappedFromIntentNode("node-office").zoneType("ACCESS")
                                .description("Office user zone").build(),
                        ZoneSchema.builder().id("zone-guest").name("guest")
                                .mappedFromIntentNode("node-guest").zoneType("ACCESS")
                                .description("Guest user zone").build(),
                        ZoneSchema.builder().id("zone-server").name("server")
                                .mappedFromIntentNode("node-server").zoneType("SERVICE")
                                .description("Server zone").build(),
                        ZoneSchema.builder().id("zone-core").name("core")
                                .zoneType("CORE").description("Core infrastructure zone").build()
                ))
                .addressPlan(List.of(
                        AddressPlanItemSchema.builder().id("addr-office").zoneId("zone-office")
                                .subnet("10.1.0.0/24").gateway("10.1.0.1")
                                .dnsServers(List.of("8.8.8.8")).exampleHostAddress("10.1.0.100")
                                .traceIntentNodeIds(List.of("node-office")).build(),
                        AddressPlanItemSchema.builder().id("addr-guest").zoneId("zone-guest")
                                .subnet("10.2.0.0/24").gateway("10.2.0.1")
                                .dnsServers(List.of("8.8.8.8")).exampleHostAddress("10.2.0.100")
                                .traceIntentNodeIds(List.of("node-guest")).build(),
                        AddressPlanItemSchema.builder().id("addr-server").zoneId("zone-server")
                                .subnet("10.3.0.0/24").gateway("10.3.0.1")
                                .dnsServers(List.of("8.8.8.8")).exampleHostAddress("10.3.0.10")
                                .traceIntentNodeIds(List.of("node-server")).build()
                ))
                .vlanPlan(List.of(
                        VlanPlanItemSchema.builder().id("vlan-office").vlanId(100).name("office")
                                .zoneId("zone-office").build(),
                        VlanPlanItemSchema.builder().id("vlan-guest").vlanId(110).name("guest")
                                .zoneId("zone-guest").build(),
                        VlanPlanItemSchema.builder().id("vlan-server").vlanId(120).name("server")
                                .zoneId("zone-server").build()
                ))
                .routingPlan(RoutingPlanSchema.builder()
                        .id("routing-ospf")
                        .protocol("OSPF")
                        .area("0.0.0.0")
                        .routers(List.of(
                                RoutingRouterSchema.builder().id("router-edge").deviceId("rtr-edge")
                                        .routerId("10.1.0.1")
                                        .advertisedNetworks(List.of("10.1.0.0/24", "10.2.0.0/24", "10.3.0.0/24"))
                                        .build()
                        ))
                        .build())
                .securityPolicies(List.of(
                        SecurityPolicySchema.builder().id("sec-office-to-server")
                                .name("Office to Server Access").sourceZone("zone-office")
                                .targetZone("zone-server").action("ALLOW").service("business-application")
                                .enforcementDeviceId("sw-core").enforcementInterfaceName("Gi0/3")
                                .enforcementDirection("inbound").basedOnIntentRelation("rel-office-server")
                                .traceIntentNodeIds(List.of("node-office", "node-server"))
                                .traceIntentRelationIds(List.of("rel-office-server")).build(),
                        SecurityPolicySchema.builder().id("sec-guest-to-server")
                                .name("Guest to Server Deny").sourceZone("zone-guest")
                                .targetZone("zone-server").action("DENY").service("any")
                                .enforcementDeviceId("sw-core").enforcementInterfaceName("Gi0/3")
                                .enforcementDirection("inbound").basedOnIntentRelation("rel-guest-server")
                                .traceIntentNodeIds(List.of("node-guest", "node-server"))
                                .traceIntentRelationIds(List.of("rel-guest-server")).build(),
                        SecurityPolicySchema.builder().id("sec-office-guest-isolation")
                                .name("Office-Guest Isolation").sourceZone("zone-office")
                                .targetZone("zone-guest").action("DENY").service("any")
                                .enforcementDeviceId("sw-core").enforcementDirection("inbound")
                                .basedOnIntentRelation("rel-office-guest")
                                .traceIntentNodeIds(List.of("node-office", "node-guest"))
                                .traceIntentRelationIds(List.of("rel-office-guest")).build()
                ))
                .planConstraints(List.of(
                        PlanConstraintSchema.builder().id("con-private-addressing").type("addressing")
                                .description("Use RFC 1918 private addressing for all internal zones")
                                .sourceIntentId("con-guest-isolation").build()
                ))
                .warnings(List.of("Internet connectivity not explicitly requested; NAT plan not generated."))
                .build();
    }

    public static NetworkPlan validPlan() {
        return new PlanningResponseParser().parse(enterprisePlanSchema(), context());
    }

    static TraceRefs intentRefs(List<String> intentNodeIds) {
        TraceRefs refs = new TraceRefs();
        refs.setIntentNodeIds(intentNodeIds);
        return refs;
    }
}
