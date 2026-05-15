package com.yali.mactav.model.execution;

import com.yali.mactav.common.enums.StageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionReport {

    private String taskId;
    private Integer planVersion;
    private Integer configVersion;
    private Integer executionVersion;
    private String executionMode;
    private ExecutionPlan executionPlan;
    private RuntimeState runtimeState;
    private TestResult testResult;
    private StageStatus stageStatus;
}
