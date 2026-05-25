package com.yali.mactav.model.workspace;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.verification.ValidationReport;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务状态中心，保存各个阶段性产物
 */
/**
 * Current task workspace view plus references to versioned stage artifacts.
 *
 * <p>The DTO lives in mac-tav-model as shared state shape only. Model Core owns
 * updates and version management; professional agents must return validated
 * stage DTOs and must not mutate this object directly.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkWorkspace {

    private NetworkTask task;

    private Integer currentIntentVersion;

    private Integer currentPlanVersion;

    private Integer currentConfigVersion;

    private Integer currentExecutionVersion;

    private Integer currentValidationVersion;

    private Integer currentRepairVersion;

    @Builder.Default
    private Map<ArtifactType, String> currentArtifactRefs = new EnumMap<>(ArtifactType.class);

    private NetworkIntent currentIntent;

    private NetworkPlan currentPlan;

    private ConfigSet currentConfigSet;

    private ExecutionReport currentExecutionReport;

    private ValidationReport currentValidationReport;

    private RepairPlan currentRepairPlan;

    @Builder.Default
    private List<NetworkArtifact> artifacts = new ArrayList<>();

    @Builder.Default
    private List<AgentExecutionRecord> agentExecutionRecords = new ArrayList<>();

    @Builder.Default
    private List<WorkspaceEvent> events = new ArrayList<>();

    @Builder.Default
    private List<WorkspaceChangeRecord> changeHistory = new ArrayList<>();

    private TaskStatus workspaceStatus;
}
