package com.yali.mactav.modelcore.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * MySQL row for current workspace pointers and version state.
 */
@Data
public class NetworkWorkspaceStateEntity {

    private String taskId;
    private String workspaceStatus;
    private Integer currentIntentVersion;
    private Integer currentPlanVersion;
    private Integer currentConfigVersion;
    private Integer currentExecutionVersion;
    private Integer currentValidationVersion;
    private Integer currentRepairVersion;
    private String currentArtifactRefsJson;
    private LocalDateTime updateTime;
}
