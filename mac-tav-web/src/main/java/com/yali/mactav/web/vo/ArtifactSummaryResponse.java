package com.yali.mactav.web.vo;

import com.yali.mactav.model.enums.ArtifactStatus;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Payload-free artifact summary used by list and metadata APIs.
 */
@Data
@Builder
public class ArtifactSummaryResponse {

    private String artifactId;

    private String taskId;

    private ArtifactType artifactType;

    private Integer version;

    private WorkflowStage stage;

    private ArtifactStatus status;

    private String payloadType;

    private String payloadSummary;

    private LocalDateTime createTime;

    private String createdBy;

    private TraceRefs traceRefs;

    public static ArtifactSummaryResponse from(NetworkArtifact artifact) {
        return ArtifactSummaryResponse.builder()
                .artifactId(artifact.getArtifactId())
                .taskId(artifact.getTaskId())
                .artifactType(artifact.getArtifactType())
                .version(artifact.getVersion())
                .stage(artifact.getStage())
                .status(artifact.getStatus())
                .payloadType(artifact.getPayloadType())
                .payloadSummary(artifact.getPayloadSummary())
                .createTime(artifact.getCreateTime())
                .createdBy(artifact.getCreatedBy())
                .traceRefs(artifact.getTraceRefs())
                .build();
    }
}
