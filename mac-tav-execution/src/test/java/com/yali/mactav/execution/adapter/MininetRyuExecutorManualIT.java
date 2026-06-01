package com.yali.mactav.execution.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.yali.mactav.execution.client.MininetRyuExecutorClient;
import com.yali.mactav.execution.client.dto.MininetRyuHealthResponse;
import com.yali.mactav.execution.config.ExecutionProperties;
import com.yali.mactav.execution.model.ExecutionRequest;
import com.yali.mactav.model.execution.ExecutionAction;
import com.yali.mactav.model.execution.ExecutionActionType;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.ExecutionStatus;
import com.yali.mactav.model.execution.TestCommand;
import com.yali.mactav.model.execution.TestResult;
import com.yali.mactav.model.execution.TestResultStatus;
import com.yali.mactav.model.execution.TestResultType;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Manual Mininet/Ryu integration test against the local executor tunnel.
 */
class MininetRyuExecutorManualIT {

    private static final String RUN_IT_ENV = "MACTAV_RUN_MININET_RYU_IT";

    private static final String EXECUTOR_URL = "http://127.0.0.1:18091";

    @Test
    void mininetRyuAdapterShouldRunH1S1H2ThroughLocalExecutor() {
        assumeTrue("true".equalsIgnoreCase(System.getenv(RUN_IT_ENV)),
                "Set MACTAV_RUN_MININET_RYU_IT=true to run the real Mininet/Ryu manual integration test.");
        ExecutionProperties properties = properties();
        MininetRyuExecutorClient client = new MininetRyuExecutorClient(properties);
        MininetRyuHealthResponse health = client.health();
        assertTrue(health.status() == null || !"unhealthy".equalsIgnoreCase(health.status()),
                "Executor health is not ready: " + health.status());

        ExecutionPlan plan = h1s1h2Plan();
        MininetRyuExecutionAdapter adapter = new MininetRyuExecutionAdapter(client);
        try {
            ExecutionReport report = adapter.execute(new ExecutionRequest(
                    "task-manual-mininet-ryu",
                    null,
                    null,
                    plan,
                    1,
                    ExecutionMode.MININET_RYU,
                    ExecutionEnvironmentType.MININET_RYU,
                    traceRefs(),
                    Map.of()));

            assertEquals(ExecutionStatus.SUCCESS, report.getOverallStatus(), summarize(report));
            assertPassed(report, TestResultType.CONTROLLER_STATE);
            assertPassed(report, TestResultType.TOPOLOGY_STATE);
            assertPassed(report, TestResultType.PING);
        } finally {
            client.cleanup(
                    "execution-manual-cleanup",
                    "task-manual-mininet-ryu",
                    List.of(),
                    plan.getTraceRefs());
        }
    }

    private ExecutionProperties properties() {
        ExecutionProperties properties = new ExecutionProperties();
        properties.setMode(ExecutionMode.MININET_RYU);
        ExecutionProperties.MininetRyuProperties mininetRyu = properties.getMininetRyu();
        mininetRyu.setEnabled(true);
        mininetRyu.setBaseUrl(EXECUTOR_URL);
        mininetRyu.setConnectTimeoutMs(3000);
        mininetRyu.setReadTimeoutMs(60000);
        return properties;
    }

    private ExecutionPlan h1s1h2Plan() {
        return ExecutionPlan.builder()
                .executionPlanId("execution-plan-manual-h1-s1-h2")
                .taskId("task-manual-mininet-ryu")
                .planId("plan-manual-h1-s1-h2")
                .configSetId("config-manual-h1-s1-h2")
                .targetEnvironment(ExecutionEnvironmentType.MININET_RYU)
                .executionMode(ExecutionMode.MININET_RYU)
                .topology(topology())
                .actions(List.of(
                        action("action-ryu-controller-check", ExecutionActionType.RYU_CONTROLLER_CHECK),
                        action("action-topology-state-check", ExecutionActionType.TOPOLOGY_STATE_CHECK)))
                .cleanupActions(List.of(action("action-mininet-cleanup", ExecutionActionType.MININET_CLEANUP)))
                .testCommands(List.of(TestCommand.builder()
                        .testId("test-ping-h1-h2")
                        .testType(TestResultType.PING)
                        .sourceNode("h1")
                        .targetNode("h2")
                        .parameters(Map.of("count", 3))
                        .traceRefs(traceRefs())
                        .build()))
                .traceRefs(traceRefs())
                .build();
    }

    private Topology topology() {
        return Topology.builder()
                .nodes(List.of(
                        TopologyNode.builder()
                                .id("h1")
                                .nodeType("host")
                                .ipAddress("10.0.0.1/24")
                                .traceRefs(traceRefs())
                                .build(),
                        TopologyNode.builder()
                                .id("s1")
                                .nodeType("switch")
                                .traceRefs(traceRefs())
                                .build(),
                        TopologyNode.builder()
                                .id("h2")
                                .nodeType("host")
                                .ipAddress("10.0.0.2/24")
                                .traceRefs(traceRefs())
                                .build()))
                .links(List.of(
                        TopologyLink.builder()
                                .id("l1")
                                .sourceNode("h1")
                                .targetNode("s1")
                                .traceRefs(traceRefs())
                                .build(),
                        TopologyLink.builder()
                                .id("l2")
                                .sourceNode("s1")
                                .targetNode("h2")
                                .traceRefs(traceRefs())
                                .build()))
                .build();
    }

    private ExecutionAction action(String actionId, ExecutionActionType actionType) {
        return ExecutionAction.builder()
                .actionId(actionId)
                .actionType(actionType)
                .traceRefs(traceRefs())
                .build();
    }

    private TraceRefs traceRefs() {
        return TraceRefs.builder()
                .planElementIds(List.of("manual-plan-h1-s1-h2"))
                .configBlockIds(List.of("manual-config-h1-s1-h2"))
                .testIds(List.of("test-ping-h1-h2"))
                .build();
    }

    private void assertPassed(ExecutionReport report, TestResultType type) {
        assertTrue(report.getTestResults().stream()
                        .filter(result -> result.getTestType() == type)
                        .anyMatch(result -> result.getStatus() == TestResultStatus.PASSED),
                "Expected a PASSED " + type + " result, got: " + summarize(report));
    }

    private String summarize(ExecutionReport report) {
        List<String> tests = report.getTestResults().stream()
                .map(this::summarize)
                .toList();
        List<String> errors = report.getErrors().stream()
                .map(error -> error.getErrorCode() + ":" + error.getMessage())
                .toList();
        return "status=" + report.getOverallStatus() + ", tests=" + tests + ", errors=" + errors;
    }

    private String summarize(TestResult result) {
        return result.getTestId() + "/" + result.getTestType() + "=" + result.getStatus();
    }
}
