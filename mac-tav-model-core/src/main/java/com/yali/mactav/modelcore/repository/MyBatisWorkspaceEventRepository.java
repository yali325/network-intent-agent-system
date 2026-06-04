package com.yali.mactav.modelcore.repository;

import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.modelcore.entity.WorkspaceEventEntity;
import com.yali.mactav.modelcore.mapper.WorkspaceEventMapper;
import java.time.LocalDateTime;
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

    public List<WorkspaceEventEntity> listByQuery(String taskId,
                                                  WorkflowStage stage,
                                                  String eventType,
                                                  LocalDateTime from,
                                                  LocalDateTime to,
                                                  int limit,
                                                  int offset) {
        return mapper.listByQuery(taskId, stage == null ? null : stage.name(), eventType, from, to, limit, offset);
    }

    public long countByQuery(String taskId,
                             WorkflowStage stage,
                             String eventType,
                             LocalDateTime from,
                             LocalDateTime to) {
        return mapper.countByQuery(taskId, stage == null ? null : stage.name(), eventType, from, to);
    }
}
