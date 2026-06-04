package com.yali.mactav.modelcore.repository;

import com.yali.mactav.modelcore.entity.WorkflowJobEntity;
import com.yali.mactav.modelcore.mapper.WorkflowJobMapper;
import java.util.List;
import java.util.Optional;

/**
 * Repository wrapper for basic workflow_job CRUD.
 */
public class MyBatisWorkflowJobRepository {

    private final WorkflowJobMapper mapper;

    public MyBatisWorkflowJobRepository(WorkflowJobMapper mapper) {
        this.mapper = mapper;
    }

    public WorkflowJobEntity create(WorkflowJobEntity entity) {
        mapper.insert(entity);
        return entity;
    }

    public WorkflowJobEntity update(WorkflowJobEntity entity) {
        mapper.update(entity);
        return entity;
    }

    public Optional<WorkflowJobEntity> findByJobId(String jobId) {
        return Optional.ofNullable(mapper.findByJobId(jobId));
    }

    public List<WorkflowJobEntity> listByTaskId(String taskId) {
        return mapper.listByTaskId(taskId);
    }

    public Optional<WorkflowJobEntity> findActiveByTaskId(String taskId) {
        return Optional.ofNullable(mapper.findActiveByTaskId(taskId));
    }

    public List<WorkflowJobEntity> listActiveJobs() {
        return mapper.listActiveJobs();
    }
}
