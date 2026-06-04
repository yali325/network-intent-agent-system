package com.yali.mactav.modelcore.service;

import com.yali.mactav.model.workflow.job.WorkflowJob;
import java.util.List;
import java.util.Optional;

/**
 * Model Core boundary for durable workflow_job history and status updates.
 */
public interface WorkflowJobService {

    WorkflowJob createPending(WorkflowJob job);

    WorkflowJob markRunning(String jobId);

    WorkflowJob markSuccess(String jobId);

    WorkflowJob markFailed(String jobId, String errorCode, String errorMessage);

    Optional<WorkflowJob> findByJobId(String jobId);

    List<WorkflowJob> listByTaskId(String taskId);

    Optional<WorkflowJob> findActiveByTaskId(String taskId);

    List<WorkflowJob> listActiveJobs();
}
