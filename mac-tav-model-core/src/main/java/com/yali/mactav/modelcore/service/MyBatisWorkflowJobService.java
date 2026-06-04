package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.model.workflow.job.WorkflowJobStatus;
import com.yali.mactav.modelcore.entity.WorkflowJobEntity;
import com.yali.mactav.modelcore.repository.MyBatisWorkflowJobRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis-backed WorkflowJobService for durable async job history.
 */
public class MyBatisWorkflowJobService implements WorkflowJobService {

    private final MyBatisWorkflowJobRepository repository;

    public MyBatisWorkflowJobService(MyBatisWorkflowJobRepository repository) {
        this.repository = repository;
    }

    @Override
    public WorkflowJob createPending(WorkflowJob job) {
        LocalDateTime now = LocalDateTime.now();
        job.setJobStatus(WorkflowJobStatus.PENDING);
        job.setCreateTime(job.getCreateTime() == null ? now : job.getCreateTime());
        job.setUpdateTime(now);
        return WorkflowJobAssembler.toDto(repository.create(WorkflowJobAssembler.toEntity(job)));
    }

    @Override
    public WorkflowJob markRunning(String jobId) {
        WorkflowJob job = require(jobId);
        job.setJobStatus(WorkflowJobStatus.RUNNING);
        job.setStartTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        return update(job);
    }

    @Override
    public WorkflowJob markSuccess(String jobId) {
        WorkflowJob job = require(jobId);
        job.setJobStatus(WorkflowJobStatus.SUCCESS);
        job.setFinishTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        return update(job);
    }

    @Override
    public WorkflowJob markFailed(String jobId, String errorCode, String errorMessage) {
        WorkflowJob job = require(jobId);
        job.setJobStatus(WorkflowJobStatus.FAILED);
        job.setErrorCode(errorCode);
        job.setErrorMessage(errorMessage);
        job.setFinishTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        return update(job);
    }

    @Override
    public Optional<WorkflowJob> findByJobId(String jobId) {
        return repository.findByJobId(jobId).map(WorkflowJobAssembler::toDto);
    }

    @Override
    public List<WorkflowJob> listByTaskId(String taskId) {
        return repository.listByTaskId(taskId).stream().map(WorkflowJobAssembler::toDto).toList();
    }

    @Override
    public Optional<WorkflowJob> findActiveByTaskId(String taskId) {
        return repository.findActiveByTaskId(taskId).map(WorkflowJobAssembler::toDto);
    }

    @Override
    public List<WorkflowJob> listActiveJobs() {
        return repository.listActiveJobs().stream().map(WorkflowJobAssembler::toDto).toList();
    }

    private WorkflowJob update(WorkflowJob job) {
        WorkflowJobEntity entity = repository.update(WorkflowJobAssembler.toEntity(job));
        return WorkflowJobAssembler.toDto(entity);
    }

    private WorkflowJob require(String jobId) {
        return findByJobId(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Workflow job not found: " + jobId));
    }
}
