package com.yali.mactav.model.healing;

import com.yali.mactav.model.enums.StageStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Healing 阶段产物对象
 */
/**
 * Healing-stage artifact containing diagnosis and proposed repair actions.
 *
 * <p>RepairPlan is advisory output for Orchestrator. It must not directly modify
 * NetworkWorkspace or execute repair commands.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairPlan {

    private String taskId;

    private Integer validationVersion;

    private Integer repairVersion;

    private String overallRepairStrategy;

    @Builder.Default
    private List<FailureAnalysis> failureAnalysis = new ArrayList<>();

    @Builder.Default
    private List<RepairAction> actions = new ArrayList<>();

    private Boolean requiresUserConfirmation;

    private StageStatus stageStatus;

    private LocalDateTime createTime;
}
