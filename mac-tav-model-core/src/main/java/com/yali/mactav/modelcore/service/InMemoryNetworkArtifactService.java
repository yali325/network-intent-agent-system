package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.modelcore.artifact.NetworkArtifactFactory;
import com.yali.mactav.modelcore.query.ArtifactQuery;
import com.yali.mactav.modelcore.query.QueryPageSupport;
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
    public Optional<NetworkArtifact> findByTaskIdTypeVersion(String taskId,
                                                            ArtifactType artifactType,
                                                            Integer version) {
        workspaceStateValidator.validateTaskId(taskId);
        if (artifactType == null || version == null) {
            return Optional.empty();
        }
        return artifactRepository.listByTaskIdAndType(taskId, artifactType).stream()
                .filter(artifact -> version.equals(artifact.getVersion()))
                .findFirst();
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
    public PageResult<NetworkArtifact> listArtifacts(String taskId, ArtifactQuery query) {
        workspaceStateValidator.validateTaskId(taskId);
        ArtifactQuery normalized = query == null ? new ArtifactQuery(null, null, 1, 20) : query;
        int page = QueryPageSupport.page(normalized.page());
        int size = QueryPageSupport.size(normalized.size());
        List<NetworkArtifact> filtered = artifactRepository.listByTaskId(taskId).stream()
                .filter(artifact -> normalized.artifactType() == null
                        || normalized.artifactType() == artifact.getArtifactType())
                .filter(artifact -> normalized.stage() == null || normalized.stage() == artifact.getStage())
                .toList();
        return PageResult.<NetworkArtifact>builder()
                .items(QueryPageSupport.slice(filtered, page, size))
                .page(page)
                .size(size)
                .total(filtered.size())
                .build();
    }

    @Override
    public int nextVersion(String taskId, ArtifactType artifactType) {
        workspaceStateValidator.validateTaskId(taskId);
        return artifactRepository.nextVersion(taskId, artifactType);
    }
}
