package com.yali.mactav.model.execution;

import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String executionId;

    private String taskId;

    private String planId;

    private String configSetId;

    private Integer planVersion;

    private Integer configVersion;

    private Integer executionVersion;

    private ExecutionEnvironmentType environmentType;

    private ExecutionPlan executionPlan;

    private RuntimeState runtimeState;

    @Builder.Default
    private List<TestResult> testResults = new ArrayList<>();

    @Builder.Default
    private List<ExecutionError> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private ExecutionStatus overallStatus;

    private TraceRefs traceRefs;

    private LocalDateTime createTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime updateTime;
}
