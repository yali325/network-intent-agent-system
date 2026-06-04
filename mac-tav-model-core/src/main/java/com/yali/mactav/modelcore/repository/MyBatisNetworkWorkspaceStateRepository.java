package com.yali.mactav.modelcore.repository;

import com.yali.mactav.modelcore.entity.NetworkWorkspaceStateEntity;
import com.yali.mactav.modelcore.mapper.NetworkWorkspaceStateMapper;
import java.util.Optional;

/**
 * Repository wrapper for workspace state persistence through MyBatis.
 */
public class MyBatisNetworkWorkspaceStateRepository {

    private final NetworkWorkspaceStateMapper mapper;

    public MyBatisNetworkWorkspaceStateRepository(NetworkWorkspaceStateMapper mapper) {
        this.mapper = mapper;
    }

    public NetworkWorkspaceStateEntity insert(NetworkWorkspaceStateEntity entity) {
        mapper.insert(entity);
        return entity;
    }

    public NetworkWorkspaceStateEntity update(NetworkWorkspaceStateEntity entity) {
        mapper.update(entity);
        return entity;
    }

    public Optional<NetworkWorkspaceStateEntity> findByTaskId(String taskId) {
        return Optional.ofNullable(mapper.findByTaskId(taskId));
    }
}
