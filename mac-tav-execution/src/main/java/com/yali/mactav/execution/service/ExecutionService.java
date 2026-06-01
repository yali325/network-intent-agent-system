package com.yali.mactav.execution.service;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.Map;

/**
 * Service boundary for producing an ExecutionReport from structured execution inputs.
 */
public interface ExecutionService {

    ExecutionReport execute(ExecutionServiceRequest request);

    default ExecutionReport execute(
            String taskId,
            NetworkPlan networkPlan,
            ConfigSet configSet,
            Integer executionVersion,
            ExecutionEnvironmentType environmentType,
            ExecutionMode mode,
            TraceRefs traceRefs,
            Map<String, String> artifactRefs) {
        return execute(new ExecutionServiceRequest(
                taskId,
                networkPlan,
                configSet,
                null,
                executionVersion,
                environmentType,
                mode,
                traceRefs,
                artifactRefs));
    }

    default ExecutionReport execute(
            String taskId,
            NetworkPlan networkPlan,
            ConfigSet configSet,
            ExecutionPlan executionPlan,
            Integer executionVersion,
            ExecutionEnvironmentType environmentType,
            ExecutionMode mode,
            TraceRefs traceRefs,
            Map<String, String> artifactRefs) {
        return execute(new ExecutionServiceRequest(
                taskId,
                networkPlan,
                configSet,
                executionPlan,
                executionVersion,
                environmentType,
                mode,
                traceRefs,
                artifactRefs));
    }
}
