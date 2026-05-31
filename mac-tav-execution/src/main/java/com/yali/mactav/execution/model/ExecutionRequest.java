package com.yali.mactav.execution.model;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.HashMap;
import java.util.Map;

/**
 * Structured request passed to an ExecutionAdapter.
 *
 * <p>The request carries model DTOs, selected environment/mode, and artifact
 * references. It intentionally has no raw shell command field.</p>
 */
public class ExecutionRequest {

    private String taskId;

    private NetworkPlan networkPlan;

    private ConfigSet configSet;

    private ExecutionPlan executionPlan;

    private Integer executionVersion;

    private ExecutionMode selectedMode;

    private ExecutionEnvironmentType targetEnvironment;

    private TraceRefs traceRefs;

    private Map<String, String> artifactRefs = new HashMap<>();

    public ExecutionRequest() {
    }

    public ExecutionRequest(
            String taskId,
            NetworkPlan networkPlan,
            ConfigSet configSet,
            ExecutionPlan executionPlan,
            Integer executionVersion,
            ExecutionMode selectedMode,
            ExecutionEnvironmentType targetEnvironment,
            TraceRefs traceRefs,
            Map<String, String> artifactRefs) {
        this.taskId = taskId;
        this.networkPlan = networkPlan;
        this.configSet = configSet;
        this.executionPlan = executionPlan;
        this.executionVersion = executionVersion;
        this.selectedMode = selectedMode;
        this.targetEnvironment = targetEnvironment;
        this.traceRefs = traceRefs;
        this.artifactRefs = artifactRefs == null ? new HashMap<>() : new HashMap<>(artifactRefs);
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public NetworkPlan getNetworkPlan() {
        return networkPlan;
    }

    public void setNetworkPlan(NetworkPlan networkPlan) {
        this.networkPlan = networkPlan;
    }

    public ConfigSet getConfigSet() {
        return configSet;
    }

    public void setConfigSet(ConfigSet configSet) {
        this.configSet = configSet;
    }

    public ExecutionPlan getExecutionPlan() {
        return executionPlan;
    }

    public void setExecutionPlan(ExecutionPlan executionPlan) {
        this.executionPlan = executionPlan;
    }

    public Integer getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Integer executionVersion) {
        this.executionVersion = executionVersion;
    }

    public ExecutionMode getSelectedMode() {
        return selectedMode;
    }

    public void setSelectedMode(ExecutionMode selectedMode) {
        this.selectedMode = selectedMode;
    }

    public ExecutionEnvironmentType getTargetEnvironment() {
        return targetEnvironment;
    }

    public void setTargetEnvironment(ExecutionEnvironmentType targetEnvironment) {
        this.targetEnvironment = targetEnvironment;
    }

    public TraceRefs getTraceRefs() {
        return traceRefs;
    }

    public void setTraceRefs(TraceRefs traceRefs) {
        this.traceRefs = traceRefs;
    }

    public Map<String, String> getArtifactRefs() {
        return artifactRefs;
    }

    public void setArtifactRefs(Map<String, String> artifactRefs) {
        this.artifactRefs = artifactRefs == null ? new HashMap<>() : new HashMap<>(artifactRefs);
    }
}
