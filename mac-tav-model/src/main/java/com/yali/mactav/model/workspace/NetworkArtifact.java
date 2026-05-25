package com.yali.mactav.model.workspace;

import com.yali.mactav.model.enums.ArtifactStatus;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存所有历史阶段产物。
 */
/**
 * Versioned snapshot/reference for one stage artifact in a task workspace.
 *
 * <p>payloadJson is the long-term cross-module contract. Runtime code should
 * avoid spreading arbitrary Object payloads across module boundaries.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkArtifact {

    private String artifactId;

    private String taskId;

    private ArtifactType artifactType;

    private Integer version;

    private WorkflowStage stage;

    private ArtifactStatus status;

    private String payloadType;

    private String payloadJson;

    private String payloadSummary;

    private LocalDateTime createTime;

    private String createdBy;

    private TraceRefs traceRefs;
}
