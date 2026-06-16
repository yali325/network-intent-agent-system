package com.yali.mactav.execution.converter;

import com.yali.mactav.common.enums.ErrorCode;
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
import com.yali.mactav.model.execution.TestCommand;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Converts NetworkPlan and ConfigSet into a controlled, structure-only ExecutionPlan.
 *
 * <p>The converter never turns ConfigSet command text into executable actions.
 * It extracts topology, structured state checks, test descriptors, and trace
 * references for later adapters.</p>
 */
public class NetworkExecutionPlanConverter {

    private final ExecutionSafetyPolicy safetyPolicy;

    public NetworkExecutionPlanConverter() {
        this(new ExecutionSafetyPolicy());
    }

    public NetworkExecutionPlanConverter(ExecutionSafetyPolicy safetyPolicy) {
        this.safetyPolicy = safetyPolicy;
    }

    public ExecutionPlan convert(
            NetworkPlan networkPlan,
            ConfigSet configSet,
            Integer executionVersion,
            ExecutionMode executionMode,
            ExecutionEnvironmentType targetEnvironment,
            TraceRefs traceRefs,
            Map<String, String> artifactRefs) {
        validateRequiredInputs(networkPlan, configSet);
        ExecutionMode resolvedMode = executionMode == null ? ExecutionMode.STRUCTURE_VALIDATION : executionMode;
        ExecutionEnvironmentType resolvedEnvironment =
                targetEnvironment == null ? resolveEnvironment(networkPlan.getTargetEnvironment()) : targetEnvironment;
        if (resolvedEnvironment == null) {
            throw invalid("targetEnvironment is required");
        }
        TraceRefs planTraceRefs = mergeTraceRefs(traceRefs, networkPlan.getTraceRefs(), configSet.getTraceRefs());
        if (!hasAnyTrace(planTraceRefs)) {
            throw invalid("traceRefs must include at least one plan, config, intent, or test reference");
        }
        Topology executionTopology = prepareTopology(networkPlan, resolvedMode, planTraceRefs);

        ExecutionPlan executionPlan = ExecutionPlan.builder()
                .executionPlanId("execution-plan-" + UUID.randomUUID())
                .planId(networkPlan.getPlanId())
                .configSetId(configSet.getConfigSetId())
                .taskId(networkPlan.getTaskId())
                .targetEnvironment(resolvedEnvironment)
                .executionMode(resolvedMode)
                .topology(executionTopology)
                .topologyScriptRef(createTopologyRef(networkPlan, artifactRefs))
                .actions(createStateCheckActions(executionTopology, configSet, planTraceRefs, resolvedMode))
                .cleanupActions(createCleanupActions(networkPlan, configSet, planTraceRefs, resolvedMode))
                .testCommands(createTestCommands(networkPlan, executionTopology, configSet, planTraceRefs, resolvedMode))
                .traceRefs(planTraceRefs)
                .build();
        safetyPolicy.validate(executionPlan);
        return executionPlan;
    }

    private void validateRequiredInputs(NetworkPlan networkPlan, ConfigSet configSet) {
        if (networkPlan == null) {
            throw invalid("NetworkPlan must not be null");
        }
        if (configSet == null) {
            throw invalid("ConfigSet must not be null");
        }
        if (isBlank(networkPlan.getTaskId())) {
            throw invalid("NetworkPlan.taskId is required");
        }
        if (isBlank(networkPlan.getPlanId())) {
            throw invalid("NetworkPlan.planId is required");
        }
        if (isBlank(configSet.getTaskId())) {
            throw invalid("ConfigSet.taskId is required");
        }
        if (!networkPlan.getTaskId().equals(configSet.getTaskId())) {
            throw invalid("NetworkPlan.taskId and ConfigSet.taskId must match");
        }
        if (isBlank(configSet.getPlanId())) {
            throw invalid("ConfigSet.planId is required");
        }
        if (!networkPlan.getPlanId().equals(configSet.getPlanId())) {
            throw invalid("NetworkPlan.planId and ConfigSet.planId must match");
        }
        if (isBlank(configSet.getConfigSetId())) {
            throw invalid("ConfigSet.configSetId is required");
        }
        if (networkPlan.getTargetEnvironment() == null) {
            throw invalid("NetworkPlan.targetEnvironment is required");
        }
    }

    private void validateTopology(Topology topology) {
        if (topology == null) {
            throw invalid("NetworkPlan.topology is required");
        }
        if (topology.getNodes() == null || topology.getNodes().isEmpty()) {
            throw invalid("NetworkPlan.topology.nodes must not be empty");
        }
    }

    private ExecutionEnvironmentType resolveEnvironment(TargetEnvironment targetEnvironment) {
        if (targetEnvironment == null) {
            return null;
        }
        String raw = firstNonBlank(
                targetEnvironment.getAdapterType(),
                targetEnvironment.getSimulationTarget(),
                targetEnvironment.getConfigStyle());
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (normalized.contains("MININET") || normalized.contains("RYU")) {
            return ExecutionEnvironmentType.MININET_RYU;
        }
        if (normalized.contains("STRUCTURE")) {
            return ExecutionEnvironmentType.STRUCTURE_VALIDATION;
        }
        if (normalized.contains("SIMULATOR")) {
            return ExecutionEnvironmentType.SIMULATOR;
        }
        if (normalized.contains("LAB")) {
            return ExecutionEnvironmentType.LAB_DEVICE;
        }
        return ExecutionEnvironmentType.STRUCTURE_VALIDATION;
    }

    private Topology prepareTopology(NetworkPlan networkPlan, ExecutionMode executionMode, TraceRefs traceRefs) {
        validateTopology(networkPlan.getTopology());
        if (executionMode != ExecutionMode.MININET_RYU) {
            if (networkPlan.getTopology().getLinks() == null || networkPlan.getTopology().getLinks().isEmpty()) {
                throw invalid("NetworkPlan.topology.links must not be empty");
            }
            return networkPlan.getTopology();
        }
        Topology topology = copyTopology(networkPlan.getTopology());
        normalizeSwitchNodes(topology);
        addDerivedHostsIfMissing(topology, networkPlan, traceRefs);
        linkHostsToSwitchIfNeeded(topology, traceRefs);
        validateMininetTopology(topology);
        return topology;
    }

    private Topology copyTopology(Topology source) {
        List<TopologyNode> nodes = new ArrayList<>();
        for (TopologyNode node : safeList(source.getNodes())) {
            nodes.add(TopologyNode.builder()
                    .id(node.getId())
                    .name(node.getName())
                    .nodeType(node.getNodeType())
                    .deviceType(node.getDeviceType())
                    .hostType(node.getHostType())
                    .role(node.getRole())
                    .ipAddress(node.getIpAddress())
                    .ip(node.getIp())
                    .mac(node.getMac())
                    .defaultRoute(node.getDefaultRoute())
                    .vendor(node.getVendor())
                    .zoneId(node.getZoneId())
                    .traceRefs(node.getTraceRefs())
                    .build());
        }
        List<TopologyLink> links = new ArrayList<>();
        for (TopologyLink link : safeList(source.getLinks())) {
            links.add(TopologyLink.builder()
                    .id(link.getId())
                    .sourceNode(link.getSourceNode())
                    .sourceInterface(link.getSourceInterface())
                    .targetNode(link.getTargetNode())
                    .targetInterface(link.getTargetInterface())
                    .linkType(link.getLinkType())
                    .traceRefs(link.getTraceRefs())
                    .build());
        }
        return Topology.builder().nodes(nodes).links(links).build();
    }

    private void normalizeSwitchNodes(Topology topology) {
        for (TopologyNode node : safeList(topology.getNodes())) {
            if (!isHost(node) && isNetworkDevice(node) && !isSwitch(node)) {
                node.setNodeType("switch");
                node.setDeviceType(firstNonBlank(node.getDeviceType(), "switch"));
                node.setRole(firstNonBlank(node.getRole(), "derived-mininet-switch"));
            }
        }
    }

    private void addDerivedHostsIfMissing(Topology topology, NetworkPlan networkPlan, TraceRefs traceRefs) {
        for (TopologyNode host : hostNodes(topology)) {
            ensureHostAddress(host, networkPlan);
        }
        Set<String> existingHostZones = new LinkedHashSet<>();
        for (TopologyNode host : hostNodes(topology)) {
            String zoneKey = normalizedZoneKey(firstNonBlank(host.getZoneId(), host.getId(), host.getName()));
            if (!isBlank(zoneKey)) {
                existingHostZones.add(zoneKey);
            }
        }
        int derivedIndex = hostNodes(topology).size();
        for (Map.Entry<String, String> candidate : requiredHostZones(networkPlan).entrySet()) {
            String zoneId = candidate.getKey();
            if (isBlank(zoneId)) {
                continue;
            }
            String normalizedZoneId = normalizedZoneKey(zoneId);
            if (existingHostZones.contains(normalizedZoneId) || findHostForZone(topology, zoneId) != null) {
                continue;
            }
            String ip = hostAddressForZone(networkPlan, zoneId, derivedIndex);
            if (isBlank(ip)) {
                continue;
            }
            TraceRefs hostTraceRefs = mergeTraceRefs(traceRefs, addressTraceRefsForZone(networkPlan, zoneId));
            topology.getNodes().add(TopologyNode.builder()
                    .id("host-" + safeId(zoneId, null, derivedIndex))
                    .name(firstNonBlank(candidate.getValue(), zoneId) + "-host")
                    .nodeType("host")
                    .hostType("host")
                    .role("derived-zone-host")
                    .ipAddress(ip)
                    .zoneId(zoneId)
                    .traceRefs(hostTraceRefs)
                    .build());
            existingHostZones.add(normalizedZoneId);
            derivedIndex++;
        }
    }

    private Map<String, String> requiredHostZones(NetworkPlan networkPlan) {
        Map<String, String> zones = new LinkedHashMap<>();
        for (NetworkZone zone : safeList(networkPlan.getZones())) {
            String zoneId = firstNonBlank(zone.getId(), zone.getName());
            if (!isBlank(zoneId)) {
                zones.put(zoneId, firstNonBlank(zone.getName(), zoneId));
            }
        }
        for (SecurityPolicyPlanItem policy : safeList(networkPlan.getSecurityPolicyPlan())) {
            if (!isBlank(policy.getSourceZone())) {
                zones.putIfAbsent(policy.getSourceZone(), policy.getSourceZone());
            }
            if (!isBlank(policy.getTargetZone())) {
                zones.putIfAbsent(policy.getTargetZone(), policy.getTargetZone());
            }
        }
        return zones;
    }

    private void ensureHostAddress(TopologyNode host, NetworkPlan networkPlan) {
        if (!isBlank(firstNonBlank(host.getIpAddress(), host.getIp()))) {
            return;
        }
        String ip = hostAddressForZone(networkPlan, host.getZoneId(), 0);
        if (!isBlank(ip)) {
            host.setIpAddress(ip);
        }
    }

    private String hostAddressForZone(NetworkPlan networkPlan, String zoneId, int index) {
        for (AddressPlanItem item : safeList(networkPlan.getAddressPlan())) {
            if (!sameZone(zoneId, item.getZoneId())) {
                continue;
            }
            String direct = firstNonBlank(item.getExampleHostAddress(),
                    item.getHostAddressHints() == null || item.getHostAddressHints().isEmpty()
                            ? null : item.getHostAddressHints().get(0));
            if (!isBlank(direct)) {
                return direct;
            }
            String fromSubnet = firstHostAddress(item.getSubnet(), index);
            if (!isBlank(fromSubnet)) {
                return fromSubnet;
            }
        }
        return null;
    }

    private TraceRefs addressTraceRefsForZone(NetworkPlan networkPlan, String zoneId) {
        for (AddressPlanItem item : safeList(networkPlan.getAddressPlan())) {
            if (sameZone(zoneId, item.getZoneId())) {
                return item.getTraceRefs();
            }
        }
        return null;
    }

    private String firstHostAddress(String subnet, int index) {
        if (isBlank(subnet) || !subnet.contains("/")) {
            return null;
        }
        String base = subnet.substring(0, subnet.indexOf('/'));
        String[] parts = base.split("\\.");
        if (parts.length != 4) {
            return null;
        }
        try {
            int hostOctet = Math.min(250, Math.max(10, 10 + index));
            return parts[0] + "." + parts[1] + "." + parts[2] + "." + hostOctet + subnet.substring(subnet.indexOf('/'));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void linkHostsToSwitchIfNeeded(Topology topology, TraceRefs traceRefs) {
        List<TopologyNode> switches = switchNodes(topology);
        if (switches.isEmpty()) {
            return;
        }
        String switchId = switches.get(0).getId();
        Set<String> linkedHosts = new HashSet<>();
        for (TopologyLink link : safeList(topology.getLinks())) {
            if (!isBlank(link.getSourceNode())) {
                linkedHosts.add(link.getSourceNode());
            }
            if (!isBlank(link.getTargetNode())) {
                linkedHosts.add(link.getTargetNode());
            }
        }
        for (TopologyNode host : hostNodes(topology)) {
            if (linkedHosts.contains(host.getId())) {
                continue;
            }
            topology.getLinks().add(TopologyLink.builder()
                    .id("link-" + host.getId() + "-" + switchId)
                    .sourceNode(host.getId())
                    .targetNode(switchId)
                    .linkType("mininet-access")
                    .traceRefs(traceRefs)
                    .build());
        }
    }

    private void validateMininetTopology(Topology topology) {
        List<TopologyNode> hosts = hostNodes(topology);
        List<TopologyNode> switches = switchNodes(topology);
        if (hosts.isEmpty()) {
            throw executionPlanInvalid("Mininet/Ryu execution requires at least one host derived from topology zones/address plan");
        }
        if (switches.isEmpty()) {
            throw executionPlanInvalid("Mininet/Ryu execution requires at least one switch or router/gateway mappable to a switch");
        }
        Set<String> nodeIds = new LinkedHashSet<>();
        for (TopologyNode node : safeList(topology.getNodes())) {
            if (isBlank(node.getId())) {
                throw executionPlanInvalid("Mininet/Ryu topology node id must not be blank");
            }
            nodeIds.add(node.getId());
        }
        boolean hasHostSwitchLink = false;
        for (TopologyLink link : safeList(topology.getLinks())) {
            if (isBlank(link.getSourceNode()) || isBlank(link.getTargetNode())
                    || !nodeIds.contains(link.getSourceNode()) || !nodeIds.contains(link.getTargetNode())) {
                throw executionPlanInvalid("Mininet/Ryu topology links must reference existing node ids");
            }
            boolean sourceHost = hosts.stream().anyMatch(host -> host.getId().equals(link.getSourceNode()));
            boolean targetHost = hosts.stream().anyMatch(host -> host.getId().equals(link.getTargetNode()));
            boolean sourceSwitch = switches.stream().anyMatch(node -> node.getId().equals(link.getSourceNode()));
            boolean targetSwitch = switches.stream().anyMatch(node -> node.getId().equals(link.getTargetNode()));
            hasHostSwitchLink = hasHostSwitchLink || (sourceHost && targetSwitch) || (targetHost && sourceSwitch);
        }
        if (!hasHostSwitchLink) {
            throw executionPlanInvalid("Mininet/Ryu topology requires at least one host-to-switch link");
        }
        for (TopologyNode host : hosts) {
            if (isBlank(firstNonBlank(host.getIpAddress(), host.getIp()))) {
                throw executionPlanInvalid("Mininet/Ryu host node " + host.getId() + " requires ipAddress or ip");
            }
        }
    }

    private List<ExecutionAction> createStateCheckActions(
            Topology topology,
            ConfigSet configSet,
            TraceRefs planTraceRefs,
            ExecutionMode executionMode) {
        List<ExecutionAction> actions = new ArrayList<>();
        actions.add(action(
                "action-topology-state-check",
                10,
                ExecutionActionType.TOPOLOGY_STATE_CHECK,
                null,
                null,
                Map.of(
                        "nodeCount", topology.getNodes().size(),
                        "linkCount", topology.getLinks().size()),
                planTraceRefs));
        actions.add(action(
                "action-ryu-controller-check",
                20,
                ExecutionActionType.RYU_CONTROLLER_CHECK,
                null,
                null,
                Map.of("expectedState", executionMode == ExecutionMode.MININET_RYU ? "available" : "not-started-in-structure-validation"),
                planTraceRefs));
        actions.add(action(
                "action-ryu-flow-query",
                30,
                ExecutionActionType.RYU_FLOW_QUERY,
                null,
                null,
                Map.of("configBlockRefs", configBlockIds(configSet)),
                planTraceRefs));
        for (DeviceConfig deviceConfig : safeList(configSet.getDeviceConfigs())) {
            TraceRefs actionTraceRefs = mergeTraceRefs(planTraceRefs, deviceConfig.getTraceRefs());
            actions.add(action(
                    "action-config-trace-" + safeId(deviceConfig.getDeviceId(), deviceConfig.getDeviceName(), actions.size()),
                    100 + actions.size(),
                    ExecutionActionType.TOPOLOGY_STATE_CHECK,
                    firstNonBlank(deviceConfig.getDeviceId(), deviceConfig.getDeviceName()),
                    firstNonBlank(deviceConfig.getDeviceId(), deviceConfig.getDeviceName()),
                    Map.of(
                            "deviceName", valueOrBlank(deviceConfig.getDeviceName()),
                            "commandBlockRefs", commandBlockIds(deviceConfig)),
                    actionTraceRefs));
        }
        return actions;
    }

    private List<ExecutionAction> createCleanupActions(
            NetworkPlan networkPlan,
            ConfigSet configSet,
            TraceRefs planTraceRefs,
            ExecutionMode executionMode) {
        return List.of(action(
                "action-mininet-cleanup",
                900,
                ExecutionActionType.MININET_CLEANUP,
                null,
                null,
                Map.of(
                        "planId", networkPlan.getPlanId(),
                        "configSetId", configSet.getConfigSetId(),
                        "cleanupScope", executionMode == ExecutionMode.MININET_RYU ? "mininet-ryu" : "structure-validation-only"),
                planTraceRefs));
    }

    private List<TestCommand> createTestCommands(
            NetworkPlan networkPlan,
            Topology topology,
            ConfigSet configSet,
            TraceRefs planTraceRefs,
            ExecutionMode executionMode) {
        List<TopologyNode> hosts = hostNodes(topology);
        List<TestCommand> tests = new ArrayList<>();
        if (executionMode == ExecutionMode.MININET_RYU) {
            addRelationLevelTests(tests, networkPlan, topology, configSet, executionMode);
        }
        addTestIfPossible(tests, TestResultType.PING, hosts, "Reachability structure check", planTraceRefs, executionMode);
        addTestIfPossible(tests, TestResultType.TRACEROUTE, hosts, "Path structure check", planTraceRefs, executionMode);
        addTestIfPossible(tests, TestResultType.IPERF, hosts, "Throughput test descriptor check", planTraceRefs, executionMode);
        tests.add(TestCommand.builder()
                .testId("test-flow-table-structure")
                .testType(TestResultType.FLOW_TABLE)
                .parameters(Map.of("configBlockRefs", configBlockIds(configSet)))
                .expectedResult("Flow-table query descriptor is structurally valid; no Ryu query is executed.")
                .traceRefs(planTraceRefs)
                .build());
        tests.add(TestCommand.builder()
                .testId("test-controller-state-structure")
                .testType(TestResultType.CONTROLLER_STATE)
                .parameters(Map.of("expectedController", "not-started-in-structure-validation"))
                .expectedResult("Controller state check descriptor is structurally valid; no controller is contacted.")
                .traceRefs(planTraceRefs)
                .build());
        return tests;
    }

    private void addRelationLevelTests(
            List<TestCommand> tests,
            NetworkPlan networkPlan,
            Topology topology,
            ConfigSet configSet,
            ExecutionMode executionMode) {
        Set<String> existingTestIds = new LinkedHashSet<>();
        for (TestCommand test : tests) {
            if (test != null && !isBlank(test.getTestId())) {
                existingTestIds.add(test.getTestId());
            }
        }
        for (SecurityPolicyPlanItem policy : safeList(networkPlan.getSecurityPolicyPlan())) {
            if (policy == null) {
                continue;
            }
            if (isBlank(policy.getSourceZone()) || isBlank(policy.getTargetZone())) {
                continue;
            }
            TopologyNode source = findHostForZone(topology, policy.getSourceZone());
            TopologyNode target = findHostForZone(topology, policy.getTargetZone());
            if (source == null || target == null) {
                throw executionPlanInvalid("Unable to derive relation-level test host for security policy "
                        + valueOrBlank(policy.getId())
                        + " sourceZone=" + valueOrBlank(policy.getSourceZone())
                        + " targetZone=" + valueOrBlank(policy.getTargetZone()));
            }
            if (isBlank(firstNonBlank(source.getIpAddress(), source.getIp()))
                    || isBlank(firstNonBlank(target.getIpAddress(), target.getIp()))) {
                throw executionPlanInvalid("Relation-level test hosts require ipAddress or ip for security policy "
                        + valueOrBlank(policy.getId()));
            }
            String testId = relationTestId(policy);
            if (!existingTestIds.add(testId)) {
                testId = testId + "-" + existingTestIds.size();
                existingTestIds.add(testId);
            }
            TraceRefs traceRefs = relationTraceRefs(policy, configSet, testId);
            tests.add(TestCommand.builder()
                    .testId(testId)
                    .testType(TestResultType.PING)
                    .sourceNode(source.getId())
                    .targetNode(target.getId())
                    .parameters(relationTestParameters(policy, executionMode))
                    .expectedResult(expectedConnectivity(policy))
                    .traceRefs(traceRefs)
                    .build());
        }
    }

    private String relationTestId(SecurityPolicyPlanItem policy) {
        String relationId = firstNonBlank(policy.getBasedOnIntentRelation(),
                first(policy.getTraceRefs() == null ? null : policy.getTraceRefs().getIntentRelationIds()));
        if (!isBlank(relationId)) {
            return "test-ping-" + safeId(relationId, null, 0).replaceFirst("^rel-", "");
        }
        return "test-ping-" + safeId(policy.getSourceZone(), null, 0)
                + "-" + safeId(policy.getTargetZone(), null, 1);
    }

    private Map<String, Object> relationTestParameters(SecurityPolicyPlanItem policy, ExecutionMode executionMode) {
        Map<String, Object> parameters = new LinkedHashMap<>(testParameters(TestResultType.PING, executionMode));
        parameters.put("sourceZone", valueOrBlank(policy.getSourceZone()));
        parameters.put("targetZone", valueOrBlank(policy.getTargetZone()));
        parameters.put("policyId", valueOrBlank(policy.getId()));
        parameters.put("service", valueOrBlank(policy.getService()));
        parameters.put("policyAction", valueOrBlank(policy.getAction()));
        return parameters;
    }

    private String expectedConnectivity(SecurityPolicyPlanItem policy) {
        String action = policy.getAction() == null ? "" : policy.getAction().trim().toUpperCase(Locale.ROOT);
        if (action.contains("DENY") || action.contains("BLOCK") || action.contains("DROP")
                || action.contains("REJECT") || action.contains("ISOLAT")) {
            return "unreachable";
        }
        return "reachable";
    }

    private TraceRefs relationTraceRefs(SecurityPolicyPlanItem policy, ConfigSet configSet, String testId) {
        TraceRefs traceRefs = mergeTraceRefs(policy.getTraceRefs());
        addOne(traceRefs.getTestIds(), testId);
        addOne(traceRefs.getPlanElementIds(), policy.getId());
        addOne(traceRefs.getIntentRelationIds(), policy.getBasedOnIntentRelation());
        addAll(traceRefs.getConfigBlockIds(), matchingConfigBlockIds(configSet, traceRefs, policy));
        if (!hasAnyTrace(traceRefs)) {
            throw executionPlanInvalid("Relation-level test traceRefs cannot be derived for security policy "
                    + valueOrBlank(policy.getId()));
        }
        return traceRefs;
    }

    private List<String> matchingConfigBlockIds(ConfigSet configSet, TraceRefs traceRefs, SecurityPolicyPlanItem policy) {
        List<String> blockIds = new ArrayList<>();
        for (DeviceConfig deviceConfig : safeList(configSet.getDeviceConfigs())) {
            for (CommandBlock block : safeList(deviceConfig.getCommandBlocks())) {
                if (block == null || isBlank(block.getBlockId())) {
                    continue;
                }
                TraceRefs blockTraceRefs = block.getTraceRefs();
                boolean traceMatch = intersects(blockTraceRefs == null ? null : blockTraceRefs.getPlanElementIds(),
                        traceRefs.getPlanElementIds())
                        || intersects(blockTraceRefs == null ? null : blockTraceRefs.getIntentRelationIds(),
                        traceRefs.getIntentRelationIds());
                boolean textMatch = containsToken(block.getBlockId(), policy.getId())
                        || containsToken(block.getBlockType(), policy.getId())
                        || containsToken(block.getTitle(), policy.getId())
                        || containsToken(block.getExplanation(), policy.getId())
                        || containsToken(block.getBlockId(), policy.getBasedOnIntentRelation());
                if (traceMatch || textMatch) {
                    blockIds.add(block.getBlockId());
                }
            }
        }
        return blockIds;
    }

    private void addTestIfPossible(
            List<TestCommand> tests,
            TestResultType testType,
            List<TopologyNode> hosts,
            String expectedResult,
            TraceRefs traceRefs,
            ExecutionMode executionMode) {
        if (hosts.size() < 2) {
            return;
        }
        TopologyNode source = hosts.get(0);
        TopologyNode target = hosts.get(1);
        tests.add(TestCommand.builder()
                .testId("test-" + testType.name().toLowerCase(Locale.ROOT).replace('_', '-'))
                .testType(testType)
                .sourceNode(source.getId())
                .targetNode(target.getId())
                .parameters(testParameters(testType, executionMode))
                .expectedResult(executionMode == ExecutionMode.MININET_RYU
                        ? expectedResult + " against Mininet hosts."
                        : expectedResult + "; no real " + testType.name() + " command is executed.")
                .traceRefs(traceRefs)
                .build());
    }

    private List<TopologyNode> hostNodes(Topology topology) {
        List<TopologyNode> result = new ArrayList<>();
        for (TopologyNode node : safeList(topology.getNodes())) {
            if (isHost(node)) {
                result.add(node);
            }
        }
        return result;
    }

    private List<TopologyNode> switchNodes(Topology topology) {
        List<TopologyNode> result = new ArrayList<>();
        for (TopologyNode node : safeList(topology.getNodes())) {
            if (isSwitch(node)) {
                result.add(node);
            }
        }
        return result;
    }

    private Map<String, Object> testParameters(TestResultType testType, ExecutionMode executionMode) {
        if (executionMode != ExecutionMode.MININET_RYU) {
            return Map.of("mode", "structure-validation", "realExecution", false);
        }
        return switch (testType) {
            case PING -> Map.of("count", 3);
            case TRACEROUTE -> Map.of("maxHops", 8);
            case IPERF -> Map.of("durationSeconds", 3);
            default -> Map.of();
        };
    }

    private ExecutionAction action(
            String actionId,
            int sequence,
            ExecutionActionType actionType,
            String targetNodeId,
            String targetDeviceId,
            Map<String, Object> parameters,
            TraceRefs traceRefs) {
        return ExecutionAction.builder()
                .actionId(actionId)
                .sequence(sequence)
                .actionType(actionType)
                .targetNodeId(targetNodeId)
                .targetDeviceId(targetDeviceId)
                .parameters(new LinkedHashMap<>(parameters))
                .traceRefs(traceRefs)
                .build();
    }

    private String createTopologyRef(NetworkPlan networkPlan, Map<String, String> artifactRefs) {
        if (artifactRefs != null && !isBlank(artifactRefs.get("NETWORK_PLAN"))) {
            return artifactRefs.get("NETWORK_PLAN") + "#topology";
        }
        return "network-plan:" + networkPlan.getPlanId() + "#topology";
    }

    private List<String> configBlockIds(ConfigSet configSet) {
        List<String> ids = new ArrayList<>();
        for (DeviceConfig deviceConfig : safeList(configSet.getDeviceConfigs())) {
            ids.addAll(commandBlockIds(deviceConfig));
        }
        return ids;
    }

    private List<String> commandBlockIds(DeviceConfig deviceConfig) {
        List<String> ids = new ArrayList<>();
        for (CommandBlock commandBlock : safeList(deviceConfig.getCommandBlocks())) {
            if (!isBlank(commandBlock.getBlockId())) {
                ids.add(commandBlock.getBlockId());
            }
        }
        return ids;
    }

    private TraceRefs mergeTraceRefs(TraceRefs... refs) {
        TraceRefs merged = TraceRefs.builder().build();
        for (TraceRefs ref : refs) {
            if (ref == null) {
                continue;
            }
            addAll(merged.getIntentNodeIds(), ref.getIntentNodeIds());
            addAll(merged.getIntentRelationIds(), ref.getIntentRelationIds());
            addAll(merged.getPlanElementIds(), ref.getPlanElementIds());
            addAll(merged.getConfigBlockIds(), ref.getConfigBlockIds());
            addAll(merged.getTestIds(), ref.getTestIds());
            addAll(merged.getValidationItemIds(), ref.getValidationItemIds());
            addAll(merged.getRepairActionIds(), ref.getRepairActionIds());
        }
        return merged;
    }

    private void addAll(List<String> target, List<String> source) {
        if (source == null) {
            return;
        }
        for (String value : source) {
            if (!isBlank(value) && !target.contains(value)) {
                target.add(value);
            }
        }
    }

    private void addOne(List<String> target, String value) {
        if (!isBlank(value) && !target.contains(value)) {
            target.add(value);
        }
    }

    private String first(List<String> values) {
        for (String value : safeList(values)) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean hasAnyTrace(TraceRefs traceRefs) {
        return traceRefs != null
                && (!traceRefs.getIntentNodeIds().isEmpty()
                || !traceRefs.getIntentRelationIds().isEmpty()
                || !traceRefs.getPlanElementIds().isEmpty()
                || !traceRefs.getConfigBlockIds().isEmpty()
                || !traceRefs.getTestIds().isEmpty());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String safeId(String primary, String secondary, int fallback) {
        String value = firstNonBlank(primary, secondary);
        if (value == null) {
            return "item-" + fallback;
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }

    private TopologyNode findHostForZone(Topology topology, String zoneId) {
        if (isBlank(zoneId)) {
            return null;
        }
        String normalizedZoneId = normalizedZoneKey(zoneId);
        for (TopologyNode host : hostNodes(topology)) {
            if (sameZone(zoneId, host.getZoneId())
                    || normalizedZoneId.equals(normalizedZoneKey(host.getId()))
                    || normalizedZoneId.equals(normalizedZoneKey(host.getName()))
                    || containsToken(host.getId(), zoneId)
                    || containsToken(host.getName(), zoneId)) {
                return host;
            }
        }
        return null;
    }

    private boolean isHost(TopologyNode node) {
        return hasType(node, "host");
    }

    private boolean isSwitch(TopologyNode node) {
        return hasType(node, "switch");
    }

    private boolean isNetworkDevice(TopologyNode node) {
        return hasType(node, "switch")
                || hasType(node, "router")
                || hasType(node, "gateway")
                || hasType(node, "firewall")
                || hasType(node, "network");
    }

    private boolean hasType(TopologyNode node, String token) {
        if (node == null || token == null) {
            return false;
        }
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        return containsToken(node.getNodeType(), normalizedToken)
                || containsToken(node.getDeviceType(), normalizedToken)
                || containsToken(node.getHostType(), normalizedToken)
                || containsToken(node.getRole(), normalizedToken);
    }

    private boolean containsToken(String value, String token) {
        return !isBlank(value) && !isBlank(token) && value.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }

    private boolean sameText(String left, String right) {
        return !isBlank(left) && !isBlank(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean sameZone(String left, String right) {
        return !isBlank(left) && !isBlank(right) && normalizedZoneKey(left).equals(normalizedZoneKey(right));
    }

    private String normalizedZoneKey(String value) {
        if (value == null) {
            return "";
        }
        String normalized = safeId(value, null, 0);
        if (normalized.startsWith("host-zone-")) {
            normalized = normalized.substring("host-zone-".length());
        }
        else if (normalized.startsWith("host-")) {
            normalized = normalized.substring("host-".length());
        }
        if (normalized.startsWith("zone-")) {
            normalized = normalized.substring("zone-".length());
        }
        if (normalized.endsWith("-host")) {
            normalized = normalized.substring(0, normalized.length() - "-host".length());
        }
        return normalized;
    }

    private boolean intersects(List<String> left, List<String> right) {
        if (left == null || right == null) {
            return false;
        }
        for (String value : left) {
            if (!isBlank(value) && right.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.PARAM_INVALID, message);
    }

    private BusinessException executionPlanInvalid(String message) {
        return new BusinessException(ErrorCode.EXECUTION_PLAN_INVALID, message);
    }
}
