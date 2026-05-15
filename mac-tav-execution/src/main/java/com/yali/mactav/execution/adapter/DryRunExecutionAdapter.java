package com.yali.mactav.execution.adapter;

import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.EndpointConfig;
import com.yali.mactav.model.execution.ConnectivityTestResult;
import com.yali.mactav.model.execution.ExecutionCommand;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.PolicyTestResult;
import com.yali.mactav.model.execution.RuntimeLinkState;
import com.yali.mactav.model.execution.RuntimeNodeState;
import com.yali.mactav.model.execution.RuntimeState;
import com.yali.mactav.model.execution.TestCommand;
import com.yali.mactav.model.execution.TestResult;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import java.util.ArrayList;
import java.util.List;

public class DryRunExecutionAdapter implements ExecutionAdapter {

    @Override
    public ExecutionReport adapt(NetworkPlan plan, ConfigSet configSet) {
        return ExecutionReport.builder()
                .taskId(plan == null ? null : plan.getTaskId())
                .planVersion(plan == null ? null : plan.getPlanVersion())
                .configVersion(configSet == null ? null : configSet.getConfigVersion())
                .executionVersion(1)
                .executionMode("DRY_RUN")
                .executionPlan(buildExecutionPlan(configSet))
                .runtimeState(buildRuntimeState(plan))
                .testResult(buildDefaultTestResult())
                .stageStatus(StageStatus.SUCCESS)
                .build();
    }

    private ExecutionPlan buildExecutionPlan(ConfigSet configSet) {
        return ExecutionPlan.builder()
                .adapterType("DRY_RUN")
                .topologyScript("# DryRun: create R1, SW1, SW2, office-pc-1, guest-pc-1, server-1 and internet links")
                .hostCommands(buildHostCommands(configSet))
                .flowRules(List.of())
                .testCommands(List.of(
                        testCommand("test-001", "office-pc-1", "server-1", "REACHABLE"),
                        testCommand("test-002", "guest-pc-1", "server-1", "BLOCKED"),
                        testCommand("test-003", "office-pc-1", "internet", "REACHABLE"),
                        testCommand("test-004", "guest-pc-1", "internet", "REACHABLE"),
                        testCommand("test-005", "office-pc-1", "guest-pc-1", "BLOCKED")
                ))
                .build();
    }

    private List<ExecutionCommand> buildHostCommands(ConfigSet configSet) {
        List<ExecutionCommand> commands = new ArrayList<>();
        if (configSet == null || configSet.getEndpointConfigs() == null) {
            return commands;
        }
        int index = 1;
        for (EndpointConfig endpointConfig : configSet.getEndpointConfigs()) {
            if (endpointConfig.getCommands() == null) {
                continue;
            }
            for (String command : endpointConfig.getCommands()) {
                commands.add(ExecutionCommand.builder()
                        .commandId(String.format("host-cmd-%03d", index++))
                        .nodeId(endpointConfig.getNodeId())
                        .command(command)
                        .build());
            }
        }
        return commands;
    }

    private RuntimeState buildRuntimeState(NetworkPlan plan) {
        return RuntimeState.builder()
                .environmentStatus("MOCK_READY")
                .controllerConnected(true)
                .nodes(buildNodeStates(plan))
                .links(buildLinkStates(plan))
                .build();
    }

    private List<RuntimeNodeState> buildNodeStates(NetworkPlan plan) {
        if (plan == null || plan.getTopology() == null || plan.getTopology().getNodes() == null) {
            return List.of();
        }
        List<RuntimeNodeState> states = new ArrayList<>();
        for (TopologyNode node : plan.getTopology().getNodes()) {
            states.add(RuntimeNodeState.builder()
                    .nodeId(node.getId())
                    .status("UP")
                    .build());
        }
        return states;
    }

    private List<RuntimeLinkState> buildLinkStates(NetworkPlan plan) {
        if (plan == null || plan.getTopology() == null || plan.getTopology().getLinks() == null) {
            return List.of();
        }
        List<RuntimeLinkState> states = new ArrayList<>();
        for (TopologyLink link : plan.getTopology().getLinks()) {
            states.add(RuntimeLinkState.builder()
                    .linkId(link.getId())
                    .status("UP")
                    .build());
        }
        return states;
    }

    private TestResult buildDefaultTestResult() {
        return TestResult.builder()
                .connectivityTests(List.of(
                        connectivity("test-001", "office-pc-1", "server-1", "REACHABLE", "REACHABLE", true),
                        connectivity("test-003", "office-pc-1", "internet", "REACHABLE", "REACHABLE", true),
                        connectivity("test-004", "guest-pc-1", "internet", "REACHABLE", "REACHABLE", true)
                ))
                .policyTests(List.of(
                        policy("test-002", "guest-pc-1", "server-1", "BLOCKED", "BLOCKED", true),
                        policy("test-005", "office-pc-1", "guest-pc-1", "BLOCKED", "BLOCKED", true)
                ))
                .rawLogs(List.of(
                        "DryRun adapter generated topology and test commands.",
                        "All mock nodes are UP.",
                        "All mock links are UP.",
                        "All mock tests matched expected results."
                ))
                .build();
    }

    private TestCommand testCommand(String testId, String source, String target, String expected) {
        return TestCommand.builder()
                .testId(testId)
                .type("PING")
                .source(source)
                .target(target)
                .expected(expected)
                .command("dry-run ping " + source + " " + target)
                .build();
    }

    private ConnectivityTestResult connectivity(String testId, String source, String target, String expected,
                                                String actual, boolean success) {
        return ConnectivityTestResult.builder()
                .testId(testId)
                .source(source)
                .target(target)
                .expected(expected)
                .actual(actual)
                .success(success)
                .rawOutput("Mock ping " + source + " -> " + target + " actual=" + actual)
                .build();
    }

    private PolicyTestResult policy(String testId, String source, String target, String expected, String actual,
                                    boolean success) {
        return PolicyTestResult.builder()
                .testId(testId)
                .source(source)
                .target(target)
                .expected(expected)
                .actual(actual)
                .success(success)
                .rawOutput("Mock policy " + source + " -> " + target + " actual=" + actual)
                .build();
    }
}
