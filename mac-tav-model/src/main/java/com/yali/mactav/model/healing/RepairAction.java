package com.yali.mactav.model.healing;

import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.RepairStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairAction {

    private String actionId;

    private String actionType;

    private WorkflowStage targetStage;

    private String description;

    private String relatedFailureAnalysisId;

    @Builder.Default
    private List<String> inputArtifactIds = new ArrayList<>();

    private ArtifactType expectedOutputArtifactType;

    private String riskLevel;

    private String riskReason;

    private Boolean requiresApproval;

    private String approvalStatus;

    private String approvedBy;

    private LocalDateTime approvedAt;

    private String rejectedBy;

    private LocalDateTime rejectedAt;

    private String approvalComment;

    private LocalDateTime appliedAt;

    private TraceRefs traceRefs;

    private RepairStatus status;
}
