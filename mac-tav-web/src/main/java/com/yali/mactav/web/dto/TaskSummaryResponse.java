package com.yali.mactav.web.dto;

import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Compact Web response for task creation.
 *
 * <p>The summary avoids duplicating the full workspace response while still
 * exposing the task identifiers and lifecycle markers needed by API clients.</p>
 */
@Data
@Builder
public class TaskSummaryResponse {

    private String taskId;

    private TaskStatus taskStatus;

    private WorkflowStage currentStage;

    private LocalDateTime createTime;

    public static TaskSummaryResponse from(NetworkWorkspace workspace) {
        return TaskSummaryResponse.builder()
                .taskId(workspace.getTask().getTaskId())
                .taskStatus(workspace.getTask().getTaskStatus())
                .currentStage(workspace.getTask().getCurrentStage())
                .createTime(workspace.getTask().getCreateTime())
                .build();
    }
}
