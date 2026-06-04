package com.yali.mactav.modelcore.repository;

import com.yali.mactav.modelcore.entity.NetworkTaskEntity;
import com.yali.mactav.modelcore.mapper.NetworkTaskMapper;
import java.util.Optional;

/**
 * Repository wrapper for task persistence through MyBatis.
 */
public class MyBatisNetworkTaskRepository {

    private final NetworkTaskMapper mapper;

    public MyBatisNetworkTaskRepository(NetworkTaskMapper mapper) {
        this.mapper = mapper;
    }

    public NetworkTaskEntity insert(NetworkTaskEntity entity) {
        mapper.insert(entity);
        return entity;
    }

    public NetworkTaskEntity update(NetworkTaskEntity entity) {
        mapper.update(entity);
        return entity;
    }

    public Optional<NetworkTaskEntity> findByTaskId(String taskId) {
        return Optional.ofNullable(mapper.findByTaskId(taskId));
    }
}
