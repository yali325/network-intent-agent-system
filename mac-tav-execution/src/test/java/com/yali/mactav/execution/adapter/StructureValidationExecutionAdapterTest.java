package com.yali.mactav.execution.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.model.ExecutionRequest;
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
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests the no-external-execution structure validation adapter.
 */
class StructureValidationExecutionAdapterTest {

    private final StructureValidationExecutionAdapter adapter = new StructureValidationExecutionAdapter();

    @Test
    void returnsDryRunExecutionReport() {
        ExecutionReport report = adapter.execute(request(planWithAction(ExecutionActionType.PING_TEST)));

        assertEquals(ExecutionStatus.SUCCESS, report.getOverallStatus());
        assertEquals("task-1", report.getTaskId());
        assertEquals("plan-1", report.getPlanId());
        assertEquals("config-set-1", report.getConfigSetId());
        assertEquals(ExecutionEnvironmentType.STRUCTURE_VALIDATION, report.getEnvironmentType());
        assertTrue(report.getErrors().isEmpty());
        assertFalse(report.getTestResults().isEmpty());
        assertEquals(TestResultStatus.UNKNOWN, report.getTestResults().get(0).getStatus());
        assertTrue(report.getTestResults().get(0).getActualResult().contains("real test was not executed"));
        assertEquals("not-started", report.getRuntimeState().getRyuControllerStatus());
        assertEquals("not-started", report.getRuntimeState().getMininetStatus());
    }

    @Test
    void illegalActionReturnsFailedReportWithExecutionError() {
        ExecutionReport report = adapter.execute(request(planWithAction(ExecutionActionType.APPLY_DEVICE_CONFIG)));

        assertEquals(ExecutionStatus.FAILED, report.getOverallStatus());
        assertFalse(report.getErrors().isEmpty());
        assertEquals("EXECUTION_FORBIDDEN_COMMAND", report.getErrors().get(0).getErrorCode());
        assertEquals(TestResultStatus.FAILED, report.getTestResults().get(0).getStatus());
    }

    @Test
    void unsupportedModeThrowsBusinessException() {
        ExecutionRequest request = request(planWithAction(ExecutionActionType.PING_TEST));
        request.setSelectedMode(ExecutionMode.MININET_RYU);

        BusinessException exception = assertThrows(BusinessException.class, () -> adapter.execute(request));

        assertEquals("EXECUTION_ADAPTER_NOT_FOUND", exception.getErrorCode());
    }

    private ExecutionRequest request(ExecutionPlan plan) {
        return new ExecutionRequest(
                "task-1",
                null,
                null,
                plan,
                1,
                ExecutionMode.STRUCTURE_VALIDATION,
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                traceRefs(),
                Map.of());
    }

    private ExecutionPlan planWithAction(ExecutionActionType actionType) {
        return ExecutionPlan.builder()
                .executionPlanId("execution-plan-1")
                .taskId("task-1")
                .planId("plan-1")
                .configSetId("config-set-1")
                .targetEnvironment(ExecutionEnvironmentType.STRUCTURE_VALIDATION)
                .executionMode(ExecutionMode.STRUCTURE_VALIDATION)
                .topology(Topology.builder()
                        .nodes(List.of(
                                TopologyNode.builder().id("host-office").nodeType("HOST").traceRefs(traceRefs()).build(),
                                TopologyNode.builder().id("host-server").nodeType("HOST").traceRefs(traceRefs()).build()))
                        .links(List.of(TopologyLink.builder()
                                .id("link-1")
                                .sourceNode("host-office")
                                .targetNode("host-server")
                                .traceRefs(traceRefs())
                                .build()))
                        .build())
                .actions(List.of(ExecutionAction.builder()
                        .actionId("action-1")
                        .actionType(actionType)
                        .parameters(Map.of("mode", "structure-validation"))
                        .traceRefs(traceRefs())
                        .build()))
                .cleanupActions(List.of(ExecutionAction.builder()
                        .actionId("cleanup-1")
                        .actionType(ExecutionActionType.MININET_CLEANUP)
                        .traceRefs(traceRefs())
                        .build()))
                .testCommands(List.of(TestCommand.builder()
                        .testId("test-1")
                        .testType(TestResultType.PING)
                        .sourceNode("host-office")
                        .targetNode("host-server")
                        .expectedResult("dry-run descriptor only")
                        .traceRefs(traceRefs())
                        .build()))
                .traceRefs(traceRefs())
                .build();
    }

    private TraceRefs traceRefs() {
        return TraceRefs.builder()
                .planElementIds(List.of("plan-node-1"))
                .configBlockIds(List.of("config-block-1"))
                .build();
    }
}
