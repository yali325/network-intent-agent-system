package com.yali.mactav.execution.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.execution.client.MininetRyuExecutorClient;
import com.yali.mactav.execution.client.dto.MininetRyuHealthResponse;
import com.yali.mactav.execution.client.dto.MininetRyuRunResponse;
import com.yali.mactav.execution.client.dto.MininetRyuRuntimeStateResponse;
import com.yali.mactav.execution.client.dto.MininetRyuStatusResponse;
import com.yali.mactav.execution.client.dto.MininetRyuTestResultResponse;
import com.yali.mactav.execution.config.ExecutionProperties;
import com.yali.mactav.execution.model.ExecutionRequest;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionAction;
import com.yali.mactav.model.execution.ExecutionActionType;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.ExecutionStatus;
import com.yali.mactav.model.execution.TestCommand;
import com.yali.mactav.model.execution.TestResultStatus;
import com.yali.mactav.model.execution.TestResultType;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.TargetEnvironment;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests Mininet/Ryu adapter mapping without starting Python, Mininet, or Ryu.
 */
class MininetRyuExecutionAdapterTest {

    @Test
    void mapsSuccessfulExecutorResponseToExecutionReport() {
        FakeClient client = new FakeClient(successResponse());
        MininetRyuExecutionAdapter adapter = new MininetRyuExecutionAdapter(client);

        ExecutionReport report = adapter.execute(request(mininetPlan()));

        assertTrue(client.called);
        assertEquals(ExecutionStatus.SUCCESS, report.getOverallStatus());
        assertEquals(ExecutionEnvironmentType.MININET_RYU, report.getEnvironmentType());
        assertEquals("task-1", report.getTaskId());
        assertEquals("plan-1", report.getPlanId());
        assertEquals("config-set-1", report.getConfigSetId());
        assertEquals("available", report.getRuntimeState().getRyuControllerStatus());
        assertEquals(TestResultStatus.PASSED, report.getTestResults().get(0).getStatus());
    }

    @Test
    void refusesSafetyPolicyFailureBeforeClientCall() {
        FakeClient client = new FakeClient(successResponse());
        MininetRyuExecutionAdapter adapter = new MininetRyuExecutionAdapter(client);
        ExecutionPlan plan = mininetPlan();
        plan.setActions(List.of(ExecutionAction.builder()
                .actionId("action-unsafe")
                .actionType(ExecutionActionType.APPLY_DEVICE_CONFIG)
                .traceRefs(traceRefs())
                .build()));

        ExecutionReport report = adapter.execute(request(plan));

        assertFalse(client.called);
        assertEquals(ExecutionStatus.FAILED, report.getOverallStatus());
        assertEquals("EXECUTION_FORBIDDEN_COMMAND", report.getErrors().get(0).getErrorCode());
    }

    @Test
    void refusesPlanContainingShellSemanticsBeforeClientCall() {
        FakeClient client = new FakeClient(successResponse());
        MininetRyuExecutionAdapter adapter = new MininetRyuExecutionAdapter(client);
        ExecutionPlan plan = mininetPlan();
        plan.setActions(List.of(ExecutionAction.builder()
                .actionId("action-shell")
                .actionType(ExecutionActionType.PING_TEST)
                .parameters(Map.of("command", "bash -c whoami"))
                .traceRefs(traceRefs())
                .build()));

        ExecutionReport report = adapter.execute(request(plan));

        assertFalse(client.called);
        assertEquals(ExecutionStatus.FAILED, report.getOverallStatus());
        assertEquals("EXECUTION_FORBIDDEN_COMMAND", report.getErrors().get(0).getErrorCode());
    }

    @Test
    void convertsNetworkPlanAndConfigSetWhenExecutionPlanMissing() {
        FakeClient client = new FakeClient(successResponse());
        MininetRyuExecutionAdapter adapter = new MininetRyuExecutionAdapter(client);
        ExecutionRequest request = new ExecutionRequest(
                "task-1",
                networkPlan(),
                configSet(),
                null,
                2,
                ExecutionMode.MININET_RYU,
                ExecutionEnvironmentType.MININET_RYU,
                traceRefs(),
                Map.of());

        ExecutionReport report = adapter.execute(request);

        assertTrue(client.called);
        assertEquals(ExecutionStatus.SUCCESS, report.getOverallStatus());
        assertEquals(ExecutionMode.MININET_RYU, client.lastPlan.getExecutionMode());
        assertEquals(ExecutionEnvironmentType.MININET_RYU, client.lastPlan.getTargetEnvironment());
    }

    private ExecutionRequest request(ExecutionPlan plan) {
        return new ExecutionRequest(
                "task-1",
                null,
                null,
                plan,
                1,
                ExecutionMode.MININET_RYU,
                ExecutionEnvironmentType.MININET_RYU,
                traceRefs(),
                Map.of());
    }

    private ExecutionPlan mininetPlan() {
        return ExecutionPlan.builder()
                .executionPlanId("execution-plan-1")
                .taskId("task-1")
                .planId("plan-1")
                .configSetId("config-set-1")
                .targetEnvironment(ExecutionEnvironmentType.MININET_RYU)
                .executionMode(ExecutionMode.MININET_RYU)
                .topology(topology())
                .actions(List.of(ExecutionAction.builder()
                        .actionId("action-ryu-check")
                        .actionType(ExecutionActionType.RYU_CONTROLLER_CHECK)
                        .traceRefs(traceRefs())
                        .build()))
                .cleanupActions(List.of(ExecutionAction.builder()
                        .actionId("cleanup-1")
                        .actionType(ExecutionActionType.MININET_CLEANUP)
                        .traceRefs(traceRefs())
                        .build()))
                .testCommands(List.of(TestCommand.builder()
                        .testId("test-ping")
                        .testType(TestResultType.PING)
                        .sourceNode("h1")
                        .targetNode("h2")
                        .parameters(Map.of("count", 1))
                        .traceRefs(traceRefs())
                        .build()))
                .traceRefs(traceRefs())
                .build();
    }

    private NetworkPlan networkPlan() {
        return NetworkPlan.builder()
                .taskId("task-1")
                .planId("plan-1")
                .targetEnvironment(TargetEnvironment.builder()
                        .adapterType("MININET_RYU")
                        .simulationTarget("MININET_RYU")
                        .build())
                .topology(topology())
                .traceRefs(traceRefs())
                .build();
    }

    private ConfigSet configSet() {
        return ConfigSet.builder()
                .taskId("task-1")
                .planId("plan-1")
                .configSetId("config-set-1")
                .traceRefs(traceRefs())
                .build();
    }

    private Topology topology() {
        return Topology.builder()
                .nodes(List.of(
                        TopologyNode.builder().id("h1").nodeType("host").ipAddress("10.0.0.10/24").traceRefs(traceRefs()).build(),
                        TopologyNode.builder().id("s1").nodeType("switch").traceRefs(traceRefs()).build(),
                        TopologyNode.builder().id("h2").nodeType("host").ipAddress("10.0.0.11/24").traceRefs(traceRefs()).build()))
                .links(List.of(
                        TopologyLink.builder().id("l1").sourceNode("h1").targetNode("s1").traceRefs(traceRefs()).build(),
                        TopologyLink.builder().id("l2").sourceNode("s1").targetNode("h2").traceRefs(traceRefs()).build()))
                .build();
    }

    private TraceRefs traceRefs() {
        return TraceRefs.builder()
                .planElementIds(List.of("plan-node-1"))
                .configBlockIds(List.of("config-block-1"))
                .testIds(List.of("test-ping"))
                .build();
    }

    private MininetRyuRunResponse successResponse() {
        return new MininetRyuRunResponse(
                "SUCCESS",
                "execution-1",
                new MininetRyuRuntimeStateResponse(
                        "python-mininet-ryu-executor",
                        "127.0.0.1:18091",
                        "available",
                        "cleaned",
                        "EXECUTION_COMPLETED",
                        "ok",
                        OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                        OffsetDateTime.parse("2026-06-01T10:00:01Z")),
                List.of(new MininetRyuTestResultResponse(
                        "test-ping",
                        "PING",
                        "PASSED",
                        "h1",
                        "h2",
                        "h1",
                        "h2",
                        null,
                        "0% packet loss",
                        "ping passed",
                        "0% packet loss",
                        "",
                        Map.of("durationMs", 10),
                        "ping passed",
                        Map.of(),
                        OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                        OffsetDateTime.parse("2026-06-01T10:00:01Z"),
                        1000L,
                        traceRefs())),
                Map.of("status", "collected"),
                List.of(),
                "Execution completed.",
                OffsetDateTime.parse("2026-06-01T10:00:00Z"),
                OffsetDateTime.parse("2026-06-01T10:00:01Z"));
    }

    private static class FakeClient extends MininetRyuExecutorClient {
        private final MininetRyuRunResponse response;
        private boolean called;
        private ExecutionPlan lastPlan;

        FakeClient(MininetRyuRunResponse response) {
            super(new ExecutionProperties());
            this.response = response;
        }

        @Override
        public MininetRyuRunResponse run(String executionId, ExecutionPlan executionPlan, Integer executionVersion) {
            this.called = true;
            this.lastPlan = executionPlan;
            return response;
        }

        @Override
        public MininetRyuHealthResponse health() {
            return new MininetRyuHealthResponse("ok", "3.10", 18091, List.of("simple_switch_13", "ofctl_rest"), "true");
        }

        @Override
        public MininetRyuStatusResponse ryuStatus() {
            return new MininetRyuStatusResponse("available", Map.of());
        }

        @Override
        public MininetRyuStatusResponse mininetStatus() {
            return new MininetRyuStatusResponse("available", Map.of());
        }
    }
}
