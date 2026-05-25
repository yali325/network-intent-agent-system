package com.yali.mactav.model.task;

import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User-submitted network-intent task and top-level lifecycle marker.
 *
 * <p>This DTO is shared by modules and must remain free of web, orchestrator,
 * persistence, and agent-framework dependencies. State changes are coordinated
 * through orchestrator/model-core rather than by agents directly.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkTask {

    private String taskId;

    private String rawText;

    private TaskStatus taskStatus;

    private WorkflowStage currentStage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String createdBy;

    private String description;
}
