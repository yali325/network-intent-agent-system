package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.modelcore.artifact.NetworkArtifactFactory;
import com.yali.mactav.modelcore.assembler.MyBatisModelCoreAssembler;
import com.yali.mactav.modelcore.query.ArtifactQuery;
import com.yali.mactav.modelcore.query.QueryPageSupport;
import com.yali.mactav.modelcore.repository.MyBatisNetworkArtifactRepository;
import com.yali.mactav.modelcore.validator.ArtifactValidator;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis-backed NetworkArtifactService for durable artifact history.
 */
public class MyBatisNetworkArtifactService implements NetworkArtifactService {

    private final MyBatisNetworkArtifactRepository repository;
    private final NetworkArtifactFactory artifactFactory;
    private final ArtifactValidator artifactValidator;
    private final WorkspaceStateValidator workspaceStateValidator;
    private final MyBatisModelCoreAssembler assembler;

    public MyBatisNetworkArtifactService(MyBatisNetworkArtifactRepository repository,
                                         NetworkArtifactFactory artifactFactory,
                                         ArtifactValidator artifactValidator,
                                         WorkspaceStateValidator workspaceStateValidator,
                                         MyBatisModelCoreAssembler assembler) {
        this.repository = repository;
        this.artifactFactory = artifactFactory;
        this.artifactValidator = artifactValidator;
        this.workspaceStateValidator = workspaceStateValidator;
        this.assembler = assembler;
    }

    @Override
    public NetworkArtifact createArtifact(String taskId,
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
        repository.insert(assembler.toEntity(artifact));
        return artifact;
    }

    @Override
    public Optional<NetworkArtifact> findByArtifactId(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return Optional.empty();
        }
        return repository.findByArtifactId(artifactId).map(assembler::toArtifact);
    }

    @Override
    public Optional<NetworkArtifact> findByTaskIdTypeVersion(String taskId,
                                                            ArtifactType artifactType,
                                                            Integer version) {
        workspaceStateValidator.validateTaskId(taskId);
        if (artifactType == null || version == null) {
            return Optional.empty();
        }
        return repository.findByTaskIdTypeVersion(taskId, artifactType, version).map(assembler::toArtifact);
    }

    @Override
    public List<NetworkArtifact> listByTaskId(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return repository.listByTaskId(taskId).stream().map(assembler::toArtifact).toList();
    }

    @Override
    public List<NetworkArtifact> listByTaskIdAndType(String taskId, ArtifactType artifactType) {
        workspaceStateValidator.validateTaskId(taskId);
        return repository.listByTaskIdAndType(taskId, artifactType).stream().map(assembler::toArtifact).toList();
    }

    @Override
    public PageResult<NetworkArtifact> listArtifacts(String taskId, ArtifactQuery query) {
        workspaceStateValidator.validateTaskId(taskId);
        ArtifactQuery normalized = query == null ? new ArtifactQuery(null, null, 1, 20) : query;
        int page = QueryPageSupport.page(normalized.page());
        int size = QueryPageSupport.size(normalized.size());
        List<NetworkArtifact> items = repository.listByQuery(
                        taskId,
                        normalized.artifactType(),
                        normalized.stage(),
                        size,
                        QueryPageSupport.offset(page, size))
                .stream()
                .map(assembler::toArtifact)
                .toList();
        long total = repository.countByQuery(taskId, normalized.artifactType(), normalized.stage());
        return PageResult.<NetworkArtifact>builder()
                .items(items)
                .page(page)
                .size(size)
                .total(total)
                .build();
    }

    @Override
    public int nextVersion(String taskId, ArtifactType artifactType) {
        workspaceStateValidator.validateTaskId(taskId);
        return repository.nextVersion(taskId, artifactType);
    }
}
