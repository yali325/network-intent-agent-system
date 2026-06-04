package com.yali.mactav.modelcore.repository;

import com.yali.mactav.modelcore.entity.WorkspaceChangeRecordEntity;
import com.yali.mactav.modelcore.mapper.WorkspaceChangeRecordMapper;
import java.util.List;

/**
 * Repository wrapper for workspace change audit persistence through MyBatis.
 */
public class MyBatisWorkspaceChangeRecordRepository {

    private final WorkspaceChangeRecordMapper mapper;

    public MyBatisWorkspaceChangeRecordRepository(WorkspaceChangeRecordMapper mapper) {
        this.mapper = mapper;
    }

    public WorkspaceChangeRecordEntity append(WorkspaceChangeRecordEntity entity) {
        mapper.insert(entity);
        return entity;
    }

    public List<WorkspaceChangeRecordEntity> listByTaskId(String taskId) {
        return mapper.listByTaskId(taskId);
    }
}
