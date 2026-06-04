package com.yali.mactav.modelcore.service;

import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workflow.job.WorkflowJob;
import com.yali.mactav.model.workflow.job.WorkflowJobStatus;
import com.yali.mactav.model.workflow.job.WorkflowJobType;
import com.yali.mactav.modelcore.entity.WorkflowJobEntity;

/**
 * Converts workflow_job row entities to public WorkflowJob DTOs.
 */
final class WorkflowJobAssembler {

    private WorkflowJobAssembler() {
    }

    static WorkflowJob toDto(WorkflowJobEntity entity) {
        if (entity == null) {
            return null;
        }
        return WorkflowJob.builder()
                .jobId(entity.getJobId())
                .taskId(entity.getTaskId())
                .requestedStage(parseStage(entity.getRequestedStage()))
                .jobType(parseType(entity.getJobType()))
                .jobStatus(parseStatus(entity.getJobStatus()))
                .requestedBy(entity.getRequestedBy())
                .requestPayloadJson(entity.getRequestPayloadJson())
                .startTime(entity.getStartTime())
                .finishTime(entity.getFinishTime())
                .errorCode(entity.getErrorCode())
                .errorMessage(entity.getErrorMessage())
                .traceId(entity.getTraceId())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .build();
    }

    static WorkflowJobEntity toEntity(WorkflowJob job) {
        WorkflowJobEntity entity = new WorkflowJobEntity();
        entity.setJobId(job.getJobId());
        entity.setTaskId(job.getTaskId());
        entity.setRequestedStage(job.getRequestedStage() == null ? null : job.getRequestedStage().name());
        entity.setJobType(job.getJobType() == null ? null : job.getJobType().name());
        entity.setJobStatus(job.getJobStatus() == null ? null : job.getJobStatus().name());
        entity.setRequestedBy(job.getRequestedBy());
        entity.setRequestPayloadJson(job.getRequestPayloadJson());
        entity.setStartTime(job.getStartTime());
        entity.setFinishTime(job.getFinishTime());
        entity.setErrorCode(job.getErrorCode());
        entity.setErrorMessage(job.getErrorMessage());
        entity.setTraceId(job.getTraceId());
        entity.setCreateTime(job.getCreateTime());
        entity.setUpdateTime(job.getUpdateTime());
        return entity;
    }

    private static WorkflowStage parseStage(String value) {
        return value == null || value.isBlank() ? null : WorkflowStage.valueOf(value);
    }

    private static WorkflowJobType parseType(String value) {
        return value == null || value.isBlank() ? null : WorkflowJobType.valueOf(value);
    }

    private static WorkflowJobStatus parseStatus(String value) {
        return value == null || value.isBlank() ? null : WorkflowJobStatus.valueOf(value);
    }
}
