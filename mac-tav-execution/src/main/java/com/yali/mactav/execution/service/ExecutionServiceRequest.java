package com.yali.mactav.execution.service;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.HashMap;
import java.util.Map;

/**
 * Structured service request for selecting and invoking an ExecutionAdapter.
 */
public class ExecutionServiceRequest {

    private String taskId;

    private NetworkPlan networkPlan;

    private ConfigSet configSet;

    private ExecutionPlan executionPlan;

    private Integer executionVersion;

    private ExecutionEnvironmentType environmentType;

    private ExecutionMode mode;

    private TraceRefs traceRefs;

    private Map<String, String> artifactRefs = new HashMap<>();

    public ExecutionServiceRequest() {
    }

    public ExecutionServiceRequest(
            String taskId,
            NetworkPlan networkPlan,
            ConfigSet configSet,
            ExecutionPlan executionPlan,
            Integer executionVersion,
            ExecutionEnvironmentType environmentType,
            ExecutionMode mode,
            TraceRefs traceRefs,
            Map<String, String> artifactRefs) {
        this.taskId = taskId;
        this.networkPlan = networkPlan;
        this.configSet = configSet;
        this.executionPlan = executionPlan;
        this.executionVersion = executionVersion;
        this.environmentType = environmentType;
        this.mode = mode;
        this.traceRefs = traceRefs;
        this.artifactRefs = artifactRefs == null ? new HashMap<>() : new HashMap<>(artifactRefs);
    }

    public String getTaskId() {
        return taskId;
    }

    public NetworkPlan getNetworkPlan() {
        return networkPlan;
    }

    public ConfigSet getConfigSet() {
        return configSet;
    }

    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }

    public Integer getExecutionVersion() {
        return executionVersion;
    }

    public ExecutionEnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public TraceRefs getTraceRefs() {
        return traceRefs;
    }

    public Map<String, String> getArtifactRefs() {
        return artifactRefs;
    }
}
