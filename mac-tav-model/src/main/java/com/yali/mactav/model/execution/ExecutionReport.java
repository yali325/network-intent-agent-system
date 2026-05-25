package com.yali.mactav.model.execution;

import com.yali.mactav.model.enums.StageStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Execute 阶段产物对象
 */
/**
 * Execution-stage artifact returned by controlled execution adapters.
 *
 * <p>ExecutionReport records execution plans, runtime state, tests, and errors.
 * It does not decide whether the original business intent was satisfied.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionReport {

    private String taskId;

    private Integer planVersion;

    private Integer configVersion;

    private Integer executionVersion;

    private String executionMode;

    private ExecutionPlan executionPlan;

    private RuntimeState runtimeState;

    private TestResult testResult;

    @Builder.Default
    private List<ExecutionError> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private StageStatus stageStatus;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;
}
