package com.yali.mactav.configuration.agent;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.model.config.CommandBlock;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigWarning;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.config.EndpointConfig;
import com.yali.mactav.model.config.GenerationSources;
import com.yali.mactav.model.config.RollbackPlan;
import com.yali.mactav.model.config.TraceRefs;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.TargetEnvironment;
import java.util.List;

public class MockConfigurationAgent implements ConfigurationAgent {

    public static final String AGENT_NAME = "MockConfigurationAgent";

    @Override
    public AgentResult<ConfigSet> execute(AgentContext context, NetworkPlan input) {
        ConfigSet configSet = buildMockConfigSet(input);
        return AgentResult.success(configSet, "Mock configuration generated", AGENT_NAME, "CONFIGURATION");
    }

    public ConfigSet buildMockConfigSet(NetworkPlan plan) {
        String taskId = plan == null ? null : plan.getTaskId();
        Integer planVersion = plan == null || plan.getPlanVersion() == null ? 1 : plan.getPlanVersion();
        TargetEnvironment targetEnvironment = plan == null || plan.getTargetEnvironment() == null
                ? TargetEnvironment.builder().vendor("Huawei").configStyle("CLI").simulationTarget("DRY_RUN").build()
                : plan.getTargetEnvironment();
        return ConfigSet.builder()
                .taskId(taskId)
                .planVersion(planVersion)
                .configVersion(1)
                .targetEnvironment(targetEnvironment)
                .generationSummary("Generated VLAN, interface, OSPF, ACL, NAT, and endpoint configs for the demo.")
                .generationSources(List.of(GenerationSources.builder()
                        .sourceType("MOCK_TEMPLATE")
                        .sourceName("office_guest_server_template")
                        .description("Static Huawei-style demo template.")
                        .build()))
                .deviceConfigs(List.of(buildR1Config(), buildSw1Config(), buildSw2Config()))
                .endpointConfigs(buildEndpointConfigs())
                .rollbackPlan(RollbackPlan.builder()
                        .strategy("REVERSE_ORDER")
                        .blockIds(List.of(
                                "R1-NAT-001",
                                "R1-OSPF-001",
                                "R1-ACL-001",
                                "R1-IF-001",
                                "SW1-PORT-001",
                                "SW1-VLAN-001",
                                "SW2-PORT-001",
                                "SW2-VLAN-001"
                        ))
                        .description("Rollback command blocks in reverse order.")
                        .build())
                .warnings(List.of(ConfigWarning.builder()
                        .level("LOW")
                        .message("Demo configuration only; interface and NAT details must be adjusted in real networks.")
                        .build()))
                .stageStatus(StageStatus.SUCCESS)
                .build();
    }

    private DeviceConfig buildR1Config() {
        List<CommandBlock> blocks = List.of(
                block("R1-IF-001", "INTERFACE", 10,
                        "Configure VLAN gateway subinterfaces",
                        List.of(
                                "interface GigabitEthernet0/0/0.10",
                                "dot1q termination vid 10",
                                "ip address 192.168.10.1 255.255.255.0",
                                "arp broadcast enable",
                                "interface GigabitEthernet0/0/0.20",
                                "dot1q termination vid 20",
                                "ip address 192.168.20.1 255.255.255.0",
                                "arp broadcast enable",
                                "interface GigabitEthernet0/0/0.30",
                                "dot1q termination vid 30",
                                "ip address 192.168.30.1 255.255.255.0",
                                "arp broadcast enable"
                        ),
                        "Create layer-3 gateways for office, guest, and server VLANs.",
                        List.of(
                                "undo interface GigabitEthernet0/0/0.10",
                                "undo interface GigabitEthernet0/0/0.20",
                                "undo interface GigabitEthernet0/0/0.30"
                        ),
                        List.of(),
                        List.of("rel-001", "rel-002", "rel-003", "rel-004"),
                        List.of("address-office", "address-guest", "address-server",
                                "vlan-office", "vlan-guest", "vlan-server")),
                block("R1-ACL-001", "ACL", 20,
                        "Deny guest access to server",
                        List.of(
                                "acl number 3000",
                                "rule 5 deny ip source 192.168.20.0 0.0.0.255 destination 192.168.30.0 0.0.0.255",
                                "rule 10 permit ip",
                                "interface GigabitEthernet0/0/0.20",
                                "traffic-filter inbound acl 3000"
                        ),
                        "Generate ACL from policy-001 to block guest-to-server access.",
                        List.of(
                                "interface GigabitEthernet0/0/0.20",
                                "undo traffic-filter inbound",
                                "undo acl number 3000"
                        ),
                        List.of("R1-IF-001"),
                        List.of("rel-002"),
                        List.of("policy-001")),
                block("R1-OSPF-001", "ROUTING", 30,
                        "Configure OSPF",
                        List.of(
                                "ospf 1 router-id 1.1.1.1",
                                "area 0.0.0.0",
                                "network 192.168.10.0 0.0.0.255",
                                "network 192.168.20.0 0.0.0.255",
                                "network 192.168.30.0 0.0.0.255"
                        ),
                        "Advertise demo business subnets through OSPF.",
                        List.of("undo ospf 1"),
                        List.of("R1-IF-001"),
                        List.of("rel-001", "rel-003", "rel-004"),
                        List.of("routing-ospf", "routing-ospf-r1")),
                block("R1-NAT-001", "NAT", 40,
                        "Configure internet access NAT intent",
                        List.of(
                                "acl number 2000",
                                "rule 5 permit source 192.168.10.0 0.0.0.255",
                                "rule 10 permit source 192.168.20.0 0.0.0.255",
                                "interface GigabitEthernet0/0/1",
                                "nat outbound 2000"
                        ),
                        "Demo NAT configuration for office and guest internet access.",
                        List.of(
                                "interface GigabitEthernet0/0/1",
                                "undo nat outbound 2000",
                                "undo acl number 2000"
                        ),
                        List.of("R1-IF-001"),
                        List.of("rel-003", "rel-004"),
                        List.of("nat-internet-access"))
        );
        return DeviceConfig.builder()
                .deviceId("R1")
                .deviceName("Gateway router")
                .deviceType("ROUTER")
                .vendor("Huawei")
                .configText(joinCommands(blocks))
                .commandBlocks(blocks)
                .build();
    }

    private DeviceConfig buildSw1Config() {
        List<CommandBlock> blocks = List.of(
                block("SW1-VLAN-001", "VLAN", 10,
                        "Create business VLANs on SW1",
                        List.of("vlan batch 10 20 30"),
                        "Create VLANs for office, guest, and server zones.",
                        List.of("undo vlan batch 10 20 30"),
                        List.of(),
                        List.of("rel-005"),
                        List.of("vlan-office", "vlan-guest", "vlan-server")),
                block("SW1-PORT-001", "INTERFACE", 20,
                        "Configure SW1 uplink and access ports",
                        List.of(
                                "interface GigabitEthernet0/0/1",
                                "port link-type trunk",
                                "port trunk allow-pass vlan 10 20 30",
                                "interface GigabitEthernet0/0/2",
                                "port link-type trunk",
                                "port trunk allow-pass vlan 10 20 30",
                                "interface GigabitEthernet0/0/10",
                                "port link-type access",
                                "port default vlan 10",
                                "interface GigabitEthernet0/0/11",
                                "port link-type access",
                                "port default vlan 20"
                        ),
                        "Configure trunks and place office and guest hosts into their VLANs.",
                        List.of(
                                "interface GigabitEthernet0/0/1",
                                "undo port trunk allow-pass vlan 10 20 30",
                                "interface GigabitEthernet0/0/2",
                                "undo port trunk allow-pass vlan 10 20 30",
                                "interface GigabitEthernet0/0/10",
                                "undo port default vlan",
                                "interface GigabitEthernet0/0/11",
                                "undo port default vlan"
                        ),
                        List.of("SW1-VLAN-001"),
                        List.of("rel-003", "rel-004", "rel-005"),
                        List.of("link-001", "link-002", "vlan-office", "vlan-guest"))
        );
        return DeviceConfig.builder()
                .deviceId("SW1")
                .deviceName("Access switch 1")
                .deviceType("SWITCH")
                .vendor("Huawei")
                .configText(joinCommands(blocks))
                .commandBlocks(blocks)
                .build();
    }

    private DeviceConfig buildSw2Config() {
        List<CommandBlock> blocks = List.of(
                block("SW2-VLAN-001", "VLAN", 10,
                        "Create server VLANs on SW2",
                        List.of("vlan batch 10 20 30"),
                        "Keep VLAN definitions aligned with trunk links.",
                        List.of("undo vlan batch 10 20 30"),
                        List.of(),
                        List.of("rel-001", "rel-002"),
                        List.of("vlan-server")),
                block("SW2-PORT-001", "INTERFACE", 20,
                        "Configure SW2 uplink and server port",
                        List.of(
                                "interface GigabitEthernet0/0/1",
                                "port link-type trunk",
                                "port trunk allow-pass vlan 10 20 30",
                                "interface GigabitEthernet0/0/10",
                                "port link-type access",
                                "port default vlan 30"
                        ),
                        "Configure trunk uplink and server access port.",
                        List.of(
                                "interface GigabitEthernet0/0/1",
                                "undo port trunk allow-pass vlan 10 20 30",
                                "interface GigabitEthernet0/0/10",
                                "undo port default vlan"
                        ),
                        List.of("SW2-VLAN-001"),
                        List.of("rel-001", "rel-002"),
                        List.of("link-002", "link-005", "vlan-server"))
        );
        return DeviceConfig.builder()
                .deviceId("SW2")
                .deviceName("Access switch 2")
                .deviceType("SWITCH")
                .vendor("Huawei")
                .configText(joinCommands(blocks))
                .commandBlocks(blocks)
                .build();
    }

    private List<EndpointConfig> buildEndpointConfigs() {
        return List.of(
                endpoint("office-pc-1", "office", List.of(
                        "ip addr add 192.168.10.10/24 dev eth0",
                        "ip route add default via 192.168.10.1"
                )),
                endpoint("guest-pc-1", "guest", List.of(
                        "ip addr add 192.168.20.10/24 dev eth0",
                        "ip route add default via 192.168.20.1"
                )),
                endpoint("server-1", "server", List.of(
                        "ip addr add 192.168.30.10/24 dev eth0",
                        "ip route add default via 192.168.30.1"
                ))
        );
    }

    private EndpointConfig endpoint(String nodeId, String zoneId, List<String> commands) {
        return EndpointConfig.builder()
                .nodeId(nodeId)
                .nodeType("HOST")
                .zoneId(zoneId)
                .commands(commands)
                .explanation("DryRun host address and route configuration.")
                .build();
    }

    private CommandBlock block(String blockId, String blockType, Integer order, String title, List<String> commands,
                               String explanation, List<String> rollbackCommands, List<String> dependsOn,
                               List<String> intentRelationIds, List<String> planElementIds) {
        return CommandBlock.builder()
                .blockId(blockId)
                .blockType(blockType)
                .order(order)
                .title(title)
                .commands(commands)
                .explanation(explanation)
                .rollbackCommands(rollbackCommands)
                .dependsOn(dependsOn)
                .traceRefs(TraceRefs.builder()
                        .intentRelationIds(intentRelationIds)
                        .planElementIds(planElementIds)
                        .build())
                .build();
    }

    private String joinCommands(List<CommandBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        for (CommandBlock block : blocks) {
            for (String command : block.getCommands()) {
                if (builder.length() > 0) {
                    builder.append(System.lineSeparator());
                }
                builder.append(command);
            }
        }
        return builder.toString();
    }
}
