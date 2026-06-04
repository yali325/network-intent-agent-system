package com.yali.mactav.modelcore.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * MySQL row for workspace change audit records.
 */
@Data
public class WorkspaceChangeRecordEntity {

    private String changeId;
    private String taskId;
    private String stage;
    private String changeType;
    private String fromArtifactId;
    private String toArtifactId;
    private String reason;
    private String createdBy;
    private LocalDateTime createTime;
}
