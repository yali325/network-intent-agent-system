package com.yali.mactav.model.workspace;

import com.yali.mactav.common.enums.StageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentStepLog {

    private String stepId;
    private String taskId;
    private String agentName;
    private String stage;
    private StageStatus stageStatus;
    private String message;
    private String startedAt;
    private String finishedAt;
}
