package com.yali.mactav.model.workflow.job;

import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public DTO for an asynchronous workflow job history record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowJob {

    private String jobId;

    private String taskId;

    private WorkflowStage requestedStage;

    private WorkflowJobType jobType;

    private WorkflowJobStatus jobStatus;

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
