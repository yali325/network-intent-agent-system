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
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.TargetEnvironment;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

        ExecutionPlan executionPlan = ExecutionPlan.builder()
                .executionPlanId("execution-plan-" + UUID.randomUUID())
                .planId(networkPlan.getPlanId())
                .configSetId(configSet.getConfigSetId())
                .taskId(networkPlan.getTaskId())
                .targetEnvironment(resolvedEnvironment)
                .executionMode(resolvedMode)
                .topology(networkPlan.getTopology())
                .topologyScriptRef(createTopologyRef(networkPlan, artifactRefs))
                .actions(createStateCheckActions(networkPlan, configSet, planTraceRefs))
                .cleanupActions(createCleanupActions(networkPlan, configSet, planTraceRefs))
                .testCommands(createTestCommands(networkPlan, configSet, planTraceRefs))
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
        validateTopology(networkPlan.getTopology());
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
        if (topology.getLinks() == null || topology.getLinks().isEmpty()) {
            throw invalid("NetworkPlan.topology.links must not be empty");
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

    private List<ExecutionAction> createStateCheckActions(
            NetworkPlan networkPlan,
            ConfigSet configSet,
            TraceRefs planTraceRefs) {
        List<ExecutionAction> actions = new ArrayList<>();
        actions.add(action(
                "action-topology-state-check",
                10,
                ExecutionActionType.TOPOLOGY_STATE_CHECK,
                null,
                null,
                Map.of(
                        "nodeCount", networkPlan.getTopology().getNodes().size(),
                        "linkCount", networkPlan.getTopology().getLinks().size()),
                planTraceRefs));
        actions.add(action(
                "action-ryu-controller-check",
                20,
                ExecutionActionType.RYU_CONTROLLER_CHECK,
                null,
                null,
                Map.of("expectedState", "not-started-in-structure-validation"),
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
            TraceRefs planTraceRefs) {
        return List.of(action(
                "action-mininet-cleanup",
                900,
                ExecutionActionType.MININET_CLEANUP,
                null,
                null,
                Map.of(
                        "planId", networkPlan.getPlanId(),
                        "configSetId", configSet.getConfigSetId(),
                        "cleanupScope", "structure-validation-only"),
                planTraceRefs));
    }

    private List<TestCommand> createTestCommands(
            NetworkPlan networkPlan,
            ConfigSet configSet,
            TraceRefs planTraceRefs) {
        List<TopologyNode> hosts = hostNodes(networkPlan.getTopology());
        if (hosts.size() < 2) {
            hosts = safeList(networkPlan.getTopology().getNodes());
        }
        List<TestCommand> tests = new ArrayList<>();
        addTestIfPossible(tests, TestResultType.PING, hosts, "Reachability structure check", planTraceRefs);
        addTestIfPossible(tests, TestResultType.TRACEROUTE, hosts, "Path structure check", planTraceRefs);
        addTestIfPossible(tests, TestResultType.IPERF, hosts, "Throughput test descriptor check", planTraceRefs);
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

    private void addTestIfPossible(
            List<TestCommand> tests,
            TestResultType testType,
            List<TopologyNode> hosts,
            String expectedResult,
            TraceRefs traceRefs) {
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
                .parameters(Map.of("mode", "structure-validation", "realExecution", false))
                .expectedResult(expectedResult + "; no real " + testType.name() + " command is executed.")
                .traceRefs(traceRefs)
                .build());
    }

    private List<TopologyNode> hostNodes(Topology topology) {
        List<TopologyNode> result = new ArrayList<>();
        for (TopologyNode node : safeList(topology.getNodes())) {
            String type = firstNonBlank(node.getNodeType(), node.getHostType(), node.getRole());
            if (type != null && type.toLowerCase(Locale.ROOT).contains("host")) {
                result.add(node);
            }
        }
        return result;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.PARAM_INVALID, message);
    }
}
