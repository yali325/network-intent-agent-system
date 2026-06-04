package com.yali.mactav.modelcore.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * MySQL row for future asynchronous workflow jobs.
 */
@Data
public class WorkflowJobEntity {

    private String jobId;
    private String taskId;
    private String requestedStage;
    private String jobType;
    private String jobStatus;
    private String requestedBy;
    private String requestPayloadJson;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private String errorCode;
    private String errorMessage;
    private String traceId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
