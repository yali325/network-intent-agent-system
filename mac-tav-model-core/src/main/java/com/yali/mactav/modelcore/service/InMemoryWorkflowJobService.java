package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.model.workflow.job.WorkflowJobStatus;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory WorkflowJobService for explicit test/inmemory profiles.
 */
public class InMemoryWorkflowJobService implements WorkflowJobService {

    private final ConcurrentMap<String, WorkflowJob> jobs = new ConcurrentHashMap<>();

    @Override
    public WorkflowJob createPending(WorkflowJob job) {
        LocalDateTime now = LocalDateTime.now();
        job.setJobStatus(WorkflowJobStatus.PENDING);
        job.setCreateTime(job.getCreateTime() == null ? now : job.getCreateTime());
        job.setUpdateTime(now);
        WorkflowJob existing = jobs.putIfAbsent(job.getJobId(), copy(job));
        if (existing != null) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "Workflow job already exists: " + job.getJobId());
        }
        return copy(job);
    }

    @Override
    public WorkflowJob markRunning(String jobId) {
        return mutate(jobId, job -> {
            job.setJobStatus(WorkflowJobStatus.RUNNING);
            job.setStartTime(LocalDateTime.now());
        });
    }

    @Override
    public WorkflowJob markSuccess(String jobId) {
        return mutate(jobId, job -> {
            job.setJobStatus(WorkflowJobStatus.SUCCESS);
            job.setFinishTime(LocalDateTime.now());
        });
    }

    @Override
    public WorkflowJob markFailed(String jobId, String errorCode, String errorMessage) {
        return mutate(jobId, job -> {
            job.setJobStatus(WorkflowJobStatus.FAILED);
            job.setErrorCode(errorCode);
            job.setErrorMessage(errorMessage);
            job.setFinishTime(LocalDateTime.now());
        });
    }

    @Override
    public WorkflowJob markInterrupted(String jobId, String errorCode, String errorMessage) {
        return mutate(jobId, job -> {
            job.setJobStatus(WorkflowJobStatus.INTERRUPTED);
            job.setErrorCode(errorCode);
            job.setErrorMessage(errorMessage);
            job.setFinishTime(LocalDateTime.now());
        });
    }

    @Override
    public Optional<WorkflowJob> findByJobId(String jobId) {
        return Optional.ofNullable(jobs.get(jobId)).map(this::copy);
    }

    @Override
    public List<WorkflowJob> listByTaskId(String taskId) {
        return jobs.values().stream()
                .filter(job -> taskId.equals(job.getTaskId()))
                .sorted(Comparator.comparing(WorkflowJob::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::copy)
                .toList();
    }

    @Override
    public Optional<WorkflowJob> findActiveByTaskId(String taskId) {
        return listByTaskId(taskId).stream()
                .filter(job -> job.getJobStatus() == WorkflowJobStatus.PENDING
                        || job.getJobStatus() == WorkflowJobStatus.RUNNING)
                .findFirst();
    }

    @Override
    public List<WorkflowJob> listActiveJobs() {
        return jobs.values().stream()
                .filter(job -> job.getJobStatus() == WorkflowJobStatus.PENDING
                        || job.getJobStatus() == WorkflowJobStatus.RUNNING)
                .sorted(Comparator.comparing(WorkflowJob::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::copy)
                .toList();
    }

    private WorkflowJob mutate(String jobId, java.util.function.Consumer<WorkflowJob> mutation) {
        WorkflowJob updated = jobs.compute(jobId, (ignored, existing) -> {
            if (existing == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Workflow job not found: " + jobId);
            }
            WorkflowJob copy = copy(existing);
            mutation.accept(copy);
            copy.setUpdateTime(LocalDateTime.now());
            return copy;
        });
        return copy(updated);
    }

    private WorkflowJob copy(WorkflowJob source) {
        return WorkflowJob.builder()
                .jobId(source.getJobId())
                .taskId(source.getTaskId())
                .requestedStage(source.getRequestedStage())
                .jobType(source.getJobType())
                .jobStatus(source.getJobStatus())
                .requestedBy(source.getRequestedBy())
                .requestPayloadJson(source.getRequestPayloadJson())
                .startTime(source.getStartTime())
                .finishTime(source.getFinishTime())
                .errorCode(source.getErrorCode())
                .errorMessage(source.getErrorMessage())
                .traceId(source.getTraceId())
                .createTime(source.getCreateTime())
                .updateTime(source.getUpdateTime())
                .build();
    }
}
