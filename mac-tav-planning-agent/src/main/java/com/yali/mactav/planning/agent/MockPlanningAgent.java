package com.yali.mactav.planning.agent;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.AddressPlanItem;
import com.yali.mactav.model.plan.DefaultRoute;
import com.yali.mactav.model.plan.EnforcementPoint;
import com.yali.mactav.model.plan.NatPlan;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.NetworkZone;
import com.yali.mactav.model.plan.PortRef;
import com.yali.mactav.model.plan.RoutingPlan;
import com.yali.mactav.model.plan.RoutingRouterItem;
import com.yali.mactav.model.plan.SecurityPolicyPlanItem;
import com.yali.mactav.model.plan.SelectedArchitecture;
import com.yali.mactav.model.plan.TargetEnvironment;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.plan.VlanPlanItem;
import java.util.List;

public class MockPlanningAgent implements PlanningAgent {

    public static final String AGENT_NAME = "MockPlanningAgent";

    @Override
    public AgentResult<NetworkPlan> execute(AgentContext context, NetworkIntent input) {
        NetworkPlan plan = buildMockPlan(input);
        return AgentResult.success(plan, "Mock network plan generated", AGENT_NAME, "PLANNING");
    }

    public NetworkPlan buildMockPlan(NetworkIntent intent) {
        String taskId = intent == null ? null : intent.getTaskId();
        Integer intentVersion = intent == null || intent.getIntentVersion() == null ? 1 : intent.getIntentVersion();
        return NetworkPlan.builder()
                .taskId(taskId)
                .intentVersion(intentVersion)
                .planVersion(1)
                .planSummary("VLAN isolation, router-on-a-stick forwarding, ACL policies, OSPF, and NAT.")
                .selectedArchitecture(SelectedArchitecture.builder()
                        .type("ROUTER_ON_A_STICK")
                        .reason("A compact demo topology can satisfy isolation, routing, and policy goals.")
                        .build())
                .topology(buildTopology())
                .zones(buildZones())
                .addressPlan(buildAddressPlan())
                .vlanPlan(buildVlanPlan())
                .routingPlan(buildRoutingPlan())
                .securityPolicyPlan(buildSecurityPolicies())
                .natPlan(buildNatPlan())
                .targetEnvironment(TargetEnvironment.builder()
                        .vendor("Huawei")
                        .configStyle("CLI")
                        .simulationTarget("DRY_RUN")
                        .build())
                .stageStatus(StageStatus.SUCCESS)
                .build();
    }

    private Topology buildTopology() {
        return Topology.builder()
                .nodes(List.of(
                        topologyNode("R1", "Gateway router", "DEVICE", "ROUTER", null, "GATEWAY", "Huawei", null),
                        topologyNode("SW1", "Access switch 1", "DEVICE", "SWITCH", null, "ACCESS", "Huawei", null),
                        topologyNode("SW2", "Access switch 2", "DEVICE", "SWITCH", null, "ACCESS", "Huawei", null),
                        topologyNode("office-pc-1", "Office host", "HOST", null, "PC", null, null, "office"),
                        topologyNode("guest-pc-1", "Guest host", "HOST", null, "PC", null, null, "guest"),
                        topologyNode("server-1", "Server", "HOST", null, "SERVER", null, null, "server"),
                        topologyNode("internet", "Internet", "EXTERNAL_NETWORK", null, null, null, null, "internet")
                ))
                .links(List.of(
                        topologyLink("link-001", "R1", "GE0/0/0", "SW1", "GE0/0/1", "TRUNK"),
                        topologyLink("link-002", "SW1", "GE0/0/2", "SW2", "GE0/0/1", "TRUNK"),
                        topologyLink("link-003", "office-pc-1", "eth0", "SW1", "GE0/0/10", "ACCESS"),
                        topologyLink("link-004", "guest-pc-1", "eth0", "SW1", "GE0/0/11", "ACCESS"),
                        topologyLink("link-005", "server-1", "eth0", "SW2", "GE0/0/10", "ACCESS"),
                        topologyLink("link-006", "R1", "GE0/0/1", "internet", "wan0", "WAN")
                ))
                .build();
    }

    private List<NetworkZone> buildZones() {
        return List.of(
                zone("office", "Office zone", "office", "USER_ZONE"),
                zone("guest", "Guest zone", "guest", "USER_ZONE"),
                zone("server", "Server zone", "server", "SERVER_ZONE"),
                zone("internet", "Internet", "internet", "EXTERNAL_NETWORK")
        );
    }

    private List<AddressPlanItem> buildAddressPlan() {
        return List.of(
                address("address-office", "office", "192.168.10.0/24", "192.168.10.1", "192.168.10.10"),
                address("address-guest", "guest", "192.168.20.0/24", "192.168.20.1", "192.168.20.10"),
                address("address-server", "server", "192.168.30.0/24", "192.168.30.1", "192.168.30.10")
        );
    }

    private List<VlanPlanItem> buildVlanPlan() {
        return List.of(
                vlan("vlan-office", 10, "OFFICE", "office", "SW1", "GE0/0/10"),
                vlan("vlan-guest", 20, "GUEST", "guest", "SW1", "GE0/0/11"),
                vlan("vlan-server", 30, "SERVER", "server", "SW2", "GE0/0/10")
        );
    }

    private RoutingPlan buildRoutingPlan() {
        return RoutingPlan.builder()
                .id("routing-ospf")
                .protocol("OSPF")
                .area("0.0.0.0")
                .routers(List.of(RoutingRouterItem.builder()
                        .id("routing-ospf-r1")
                        .deviceId("R1")
                        .routerId("1.1.1.1")
                        .advertisedNetworks(List.of(
                                "192.168.10.0/24",
                                "192.168.20.0/24",
                                "192.168.30.0/24"
                        ))
                        .build()))
                .defaultRoute(DefaultRoute.builder()
                        .enabled(true)
                        .nextHop("ISP")
                        .build())
                .build();
    }

    private List<SecurityPolicyPlanItem> buildSecurityPolicies() {
        return List.of(
                securityPolicy("policy-001", "deny_guest_to_server", "guest", "server",
                        "rel-002", "GE0/0/0.20"),
                securityPolicy("policy-002", "deny_office_guest_access", "office", "guest",
                        "rel-005", "GE0/0/0.10")
        );
    }

    private NatPlan buildNatPlan() {
        return NatPlan.builder()
                .id("nat-internet-access")
                .enabled(true)
                .insideZones(List.of("office", "guest"))
                .outsideInterface(PortRef.builder()
                        .deviceId("R1")
                        .interfaceName("GE0/0/1")
                        .build())
                .description("Office and guest zones access the internet through R1.")
                .build();
    }

    private TopologyNode topologyNode(String id, String name, String nodeType, String deviceType, String hostType,
                                      String role, String vendor, String zoneId) {
        return TopologyNode.builder()
                .id(id)
                .name(name)
                .nodeType(nodeType)
                .deviceType(deviceType)
                .hostType(hostType)
                .role(role)
                .vendor(vendor)
                .zoneId(zoneId)
                .build();
    }

    private TopologyLink topologyLink(String id, String sourceNode, String sourceInterface, String targetNode,
                                      String targetInterface, String linkType) {
        return TopologyLink.builder()
                .id(id)
                .sourceNode(sourceNode)
                .sourceInterface(sourceInterface)
                .targetNode(targetNode)
                .targetInterface(targetInterface)
                .linkType(linkType)
                .build();
    }

    private NetworkZone zone(String id, String name, String mappedFromIntentNode, String zoneType) {
        return NetworkZone.builder()
                .id(id)
                .name(name)
                .mappedFromIntentNode(mappedFromIntentNode)
                .zoneType(zoneType)
                .build();
    }

    private AddressPlanItem address(String id, String zoneId, String subnet, String gateway, String sampleIp) {
        return AddressPlanItem.builder()
                .id(id)
                .zoneId(zoneId)
                .subnet(subnet)
                .gateway(gateway)
                .sampleIp(sampleIp)
                .build();
    }

    private VlanPlanItem vlan(String id, Integer vlanId, String name, String zoneId, String deviceId,
                              String interfaceName) {
        return VlanPlanItem.builder()
                .id(id)
                .vlanId(vlanId)
                .name(name)
                .zoneId(zoneId)
                .accessPorts(List.of(PortRef.builder()
                        .deviceId(deviceId)
                        .interfaceName(interfaceName)
                        .build()))
                .build();
    }

    private SecurityPolicyPlanItem securityPolicy(String id, String name, String sourceZone, String targetZone,
                                                  String basedOnIntentRelation, String interfaceName) {
        return SecurityPolicyPlanItem.builder()
                .id(id)
                .name(name)
                .sourceZone(sourceZone)
                .targetZone(targetZone)
                .action("DENY")
                .service("ANY")
                .enforcementPoint(EnforcementPoint.builder()
                        .deviceId("R1")
                        .interfaceName(interfaceName)
                        .direction("INBOUND")
                        .build())
                .basedOnIntentRelation(basedOnIntentRelation)
                .build();
    }
}
