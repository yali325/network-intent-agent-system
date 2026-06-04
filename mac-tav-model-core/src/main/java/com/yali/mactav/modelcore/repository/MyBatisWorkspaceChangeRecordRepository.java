package com.yali.mactav.modelcore.repository;

import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.modelcore.entity.WorkspaceChangeRecordEntity;
import com.yali.mactav.modelcore.mapper.WorkspaceChangeRecordMapper;
import java.time.LocalDateTime;
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

    public List<WorkspaceChangeRecordEntity> listByQuery(String taskId,
                                                         WorkflowStage stage,
                                                         String changeType,
                                                         LocalDateTime from,
                                                         LocalDateTime to,
                                                         int limit,
                                                         int offset) {
        return mapper.listByQuery(taskId, stage == null ? null : stage.name(), changeType, from, to, limit, offset);
    }

    public long countByQuery(String taskId,
                             WorkflowStage stage,
                             String changeType,
                             LocalDateTime from,
                             LocalDateTime to) {
        return mapper.countByQuery(taskId, stage == null ? null : stage.name(), changeType, from, to);
    }
}
