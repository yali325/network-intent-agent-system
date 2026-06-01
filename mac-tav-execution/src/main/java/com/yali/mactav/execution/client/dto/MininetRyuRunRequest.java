package com.yali.mactav.execution.client.dto;

import com.yali.mactav.model.execution.ExecutionAction;
import com.yali.mactav.model.execution.TestCommand;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;

/**
 * Structured request body sent to the Python executor run endpoint.
 */
public record MininetRyuRunRequest(
        String executionId,
        String taskId,
        String planId,
        String configSetId,
        Integer executionVersion,
        Topology topology,
        List<ExecutionAction> actions,
        List<ExecutionAction> cleanupActions,
        List<TestCommand> testCommands,
        Integer timeoutSeconds,
        TraceRefs traceRefs) {
}
