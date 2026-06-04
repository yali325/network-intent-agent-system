package com.yali.mactav.modelcore.repository;

import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.modelcore.entity.NetworkArtifactEntity;
import com.yali.mactav.modelcore.mapper.NetworkArtifactMapper;
import java.util.List;
import java.util.Optional;

/**
 * Repository wrapper for artifact history persistence through MyBatis.
 */
public class MyBatisNetworkArtifactRepository {

    private final NetworkArtifactMapper mapper;

    public MyBatisNetworkArtifactRepository(NetworkArtifactMapper mapper) {
        this.mapper = mapper;
    }

    public NetworkArtifactEntity insert(NetworkArtifactEntity entity) {
        mapper.insert(entity);
        return entity;
    }

    public NetworkArtifactEntity update(NetworkArtifactEntity entity) {
        mapper.update(entity);
        return entity;
    }

    public void markSupersededExcept(String taskId, ArtifactType artifactType, String artifactId) {
        mapper.markSupersededExcept(taskId, artifactType.name(), artifactId);
    }

    public Optional<NetworkArtifactEntity> findByArtifactId(String artifactId) {
        return Optional.ofNullable(mapper.findByArtifactId(artifactId));
    }

    public Optional<NetworkArtifactEntity> findByTaskIdTypeVersion(String taskId,
                                                                   ArtifactType artifactType,
                                                                   Integer version) {
        return Optional.ofNullable(mapper.findByTaskIdTypeVersion(taskId, artifactType.name(), version));
    }

    public List<NetworkArtifactEntity> listByTaskId(String taskId) {
        return mapper.listByTaskId(taskId);
    }

    public List<NetworkArtifactEntity> listByTaskIdAndType(String taskId, ArtifactType artifactType) {
        return mapper.listByTaskIdAndType(taskId, artifactType.name());
    }

    public int nextVersion(String taskId, ArtifactType artifactType) {
        Integer maxVersion = mapper.findMaxVersion(taskId, artifactType.name());
        return maxVersion == null ? 1 : maxVersion + 1;
    }
}
