package com.yali.mactav.modelcore.repository;

import com.yali.mactav.modelcore.entity.WorkspaceEventEntity;
import com.yali.mactav.modelcore.mapper.WorkspaceEventMapper;
import java.util.List;

/**
 * Repository wrapper for workspace event persistence through MyBatis.
 */
public class MyBatisWorkspaceEventRepository {

    private final WorkspaceEventMapper mapper;

    public MyBatisWorkspaceEventRepository(WorkspaceEventMapper mapper) {
        this.mapper = mapper;
    }

    public WorkspaceEventEntity append(WorkspaceEventEntity entity) {
        mapper.insert(entity);
        return entity;
    }

    public List<WorkspaceEventEntity> listByTaskId(String taskId) {
        return mapper.listByTaskId(taskId);
    }
}
