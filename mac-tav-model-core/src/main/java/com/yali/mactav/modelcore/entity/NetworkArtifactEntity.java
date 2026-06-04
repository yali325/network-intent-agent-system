package com.yali.mactav.modelcore.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * MySQL row for a versioned stage artifact snapshot.
 */
@Data
public class NetworkArtifactEntity {

    private String artifactId;
    private String taskId;
    private String artifactType;
    private Integer version;
    private String stage;
    private String status;
    private String payloadType;
    private String payloadJson;
    private String payloadSummary;
    private String traceRefsJson;
    private String createdBy;
    private LocalDateTime createTime;
}
