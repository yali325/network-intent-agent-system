package com.yali.mactav.modelcore.service;

import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;
import java.util.Optional;

/**
 * Model Core boundary for creating, saving, and querying versioned artifacts.
 *
 * <p>The service owns artifact metadata and history mechanics, not business DTO
 * generation, parser logic, or persistence technology choices.</p>
 */
public interface NetworkArtifactService {

    NetworkArtifact createArtifact(
            String taskId,
            ArtifactType artifactType,
            WorkflowStage stage,
            Integer version,
            Object payloadDto,
            String payloadSummary,
            String createdBy,
            TraceRefs traceRefs);

    NetworkArtifact saveArtifact(NetworkArtifact artifact);

    Optional<NetworkArtifact> findByArtifactId(String artifactId);

    List<NetworkArtifact> listByTaskId(String taskId);

    List<NetworkArtifact> listByTaskIdAndType(String taskId, ArtifactType artifactType);

    int nextVersion(String taskId, ArtifactType artifactType);
}
