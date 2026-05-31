package com.yali.mactav.execution.safety;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.execution.ExecutionAction;
import com.yali.mactav.model.execution.ExecutionActionType;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.TestCommand;
import com.yali.mactav.model.execution.TestResultType;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests Phase 6 execution safety rules without starting Mininet, Ryu, Docker, or shell commands.
 */
class ExecutionSafetyPolicyTest {

    private final ExecutionSafetyPolicy policy = new ExecutionSafetyPolicy();

    @Test
    void acceptsAllowListedStructuredActionsAndTests() {
        ExecutionPlan plan = ExecutionPlan.builder()
                .traceRefs(traceRefs())
                .actions(List.of(ExecutionAction.builder()
                        .actionId("action-1")
                        .actionType(ExecutionActionType.MININET_TOPOLOGY_START)
                        .parameters(Map.of("topologyRef", "topology-script-1"))
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
                        .sourceNode("h1")
                        .targetNode("h2")
                        .parameters(Map.of("count", 3))
                        .traceRefs(traceRefs())
                        .build()))
                .build();

        policy.validate(plan);
    }

    @Test
    void rejectsGenericActionType() {
        ExecutionPlan plan = ExecutionPlan.builder()
                .traceRefs(traceRefs())
                .actions(List.of(ExecutionAction.builder()
                        .actionId("action-1")
                        .actionType(ExecutionActionType.APPLY_DEVICE_CONFIG)
                        .traceRefs(traceRefs())
                        .build()))
                .build();

        BusinessException exception = assertThrows(BusinessException.class, () -> policy.validate(plan));

        assertEquals("EXECUTION_FORBIDDEN_COMMAND", exception.getErrorCode());
    }

    @Test
    void rejectsForbiddenCommandParameterKey() {
        ExecutionPlan plan = ExecutionPlan.builder()
                .traceRefs(traceRefs())
                .actions(List.of(ExecutionAction.builder()
                        .actionId("action-1")
                        .actionType(ExecutionActionType.PING_TEST)
                        .parameters(Map.of("command", "ping h1 h2"))
                        .traceRefs(traceRefs())
                        .build()))
                .build();

        BusinessException exception = assertThrows(BusinessException.class, () -> policy.validate(plan));

        assertEquals("EXECUTION_FORBIDDEN_COMMAND", exception.getErrorCode());
    }

    @Test
    void rejectsForbiddenShellTextInNestedParameter() {
        ExecutionPlan plan = ExecutionPlan.builder()
                .traceRefs(traceRefs())
                .actions(List.of(ExecutionAction.builder()
                        .actionId("action-1")
                        .actionType(ExecutionActionType.RYU_CONTROLLER_CHECK)
                        .parameters(Map.of("probe", Map.of("value", "bash -c whoami")))
                        .traceRefs(traceRefs())
                        .build()))
                .build();

        BusinessException exception = assertThrows(BusinessException.class, () -> policy.validate(plan));

        assertEquals("EXECUTION_FORBIDDEN_COMMAND", exception.getErrorCode());
    }

    @Test
    void rejectsMissingTraceRefs() {
        ExecutionPlan plan = ExecutionPlan.builder()
                .actions(List.of(ExecutionAction.builder()
                        .actionId("action-1")
                        .actionType(ExecutionActionType.MININET_TOPOLOGY_STOP)
                        .traceRefs(traceRefs())
                        .build()))
                .build();

        BusinessException exception = assertThrows(BusinessException.class, () -> policy.validate(plan));

        assertEquals("EXECUTION_FORBIDDEN_COMMAND", exception.getErrorCode());
    }

    private TraceRefs traceRefs() {
        return TraceRefs.builder()
                .planElementIds(List.of("plan-node-1"))
                .configBlockIds(List.of("config-block-1"))
                .build();
    }
}
