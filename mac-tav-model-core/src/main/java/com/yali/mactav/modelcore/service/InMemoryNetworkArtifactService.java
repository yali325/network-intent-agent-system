package com.yali.mactav.modelcore.service;

import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.modelcore.artifact.NetworkArtifactFactory;
import com.yali.mactav.modelcore.repository.InMemoryNetworkArtifactRepository;
import com.yali.mactav.modelcore.validator.ArtifactValidator;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.util.List;
import java.util.Optional;

/**
 * In-memory NetworkArtifactService implementation for versioned artifact history.
 *
 * <p>This service validates and stores artifact envelopes. It does not parse
 * business payloads or choose workflow stages. TODO Phase 9: replace backing
 * repository with persistent storage.</p>
 */
public class InMemoryNetworkArtifactService implements NetworkArtifactService {

    private final InMemoryNetworkArtifactRepository artifactRepository;

    private final NetworkArtifactFactory artifactFactory;

    private final ArtifactValidator artifactValidator;

    private final WorkspaceStateValidator workspaceStateValidator;

    public InMemoryNetworkArtifactService(
            InMemoryNetworkArtifactRepository artifactRepository,
            NetworkArtifactFactory artifactFactory,
            ArtifactValidator artifactValidator,
            WorkspaceStateValidator workspaceStateValidator) {
        this.artifactRepository = artifactRepository;
        this.artifactFactory = artifactFactory;
        this.artifactValidator = artifactValidator;
        this.workspaceStateValidator = workspaceStateValidator;
    }

    @Override
    public NetworkArtifact createArtifact(
            String taskId,
            ArtifactType artifactType,
            WorkflowStage stage,
            Integer version,
            Object payloadDto,
            String payloadSummary,
            String createdBy,
            TraceRefs traceRefs) {
        workspaceStateValidator.validateTaskId(taskId);
        NetworkArtifact artifact = artifactFactory.create(
                taskId,
                artifactType,
                stage,
                version,
                payloadDto,
                payloadSummary,
                createdBy,
                traceRefs);
        artifactValidator.validate(artifact);
        return artifact;
    }

    @Override
    public NetworkArtifact saveArtifact(NetworkArtifact artifact) {
        artifactValidator.validate(artifact);
        return artifactRepository.save(artifact);
    }

    @Override
    public Optional<NetworkArtifact> findByArtifactId(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return Optional.empty();
        }
        return artifactRepository.findByArtifactId(artifactId);
    }

    @Override
    public List<NetworkArtifact> listByTaskId(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return artifactRepository.listByTaskId(taskId);
    }

    @Override
    public List<NetworkArtifact> listByTaskIdAndType(String taskId, ArtifactType artifactType) {
        workspaceStateValidator.validateTaskId(taskId);
        return artifactRepository.listByTaskIdAndType(taskId, artifactType);
    }

    @Override
    public int nextVersion(String taskId, ArtifactType artifactType) {
        workspaceStateValidator.validateTaskId(taskId);
        return artifactRepository.nextVersion(taskId, artifactType);
    }
}
