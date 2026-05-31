package com.yali.mactav.execution.adapter;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.model.ExecutionRequest;
import com.yali.mactav.execution.safety.ExecutionSafetyPolicy;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionError;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.ExecutionStatus;
import com.yali.mactav.model.execution.RuntimeState;
import com.yali.mactav.model.execution.TestCommand;
import com.yali.mactav.model.execution.TestResult;
import com.yali.mactav.model.execution.TestResultStatus;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Structure-validation adapter for automated tests without Mininet/Ryu.
 *
 * <p>This adapter only validates request shape and safety policy, then returns
 * a dry-run ExecutionReport. It is not a substitute for final Mininet/Ryu
 * execution acceptance.</p>
 */
public class StructureValidationExecutionAdapter implements ExecutionAdapter {

    public static final String ADAPTER_ID = "structure-validation-execution-adapter";

    private final ExecutionSafetyPolicy safetyPolicy;

    public StructureValidationExecutionAdapter() {
        this(new ExecutionSafetyPolicy());
    }

    public StructureValidationExecutionAdapter(ExecutionSafetyPolicy safetyPolicy) {
        this.safetyPolicy = safetyPolicy;
    }

    @Override
    public String adapterId() {
        return ADAPTER_ID;
    }

    @Override
    public ExecutionEnvironmentType environmentType() {
        return ExecutionEnvironmentType.STRUCTURE_VALIDATION;
    }

    @Override
    public ExecutionMode mode() {
        return ExecutionMode.STRUCTURE_VALIDATION;
    }

    @Override
    public ExecutionReport execute(ExecutionRequest request) {
        validateRequestEnvelope(request);
        LocalDateTime startTime = LocalDateTime.now();
        ExecutionPlan executionPlan = request.getExecutionPlan();
        List<ExecutionError> errors = new ArrayList<>();
        ExecutionStatus status = ExecutionStatus.SUCCESS;
        try {
            validateExecutionPlan(executionPlan);
            safetyPolicy.validate(executionPlan);
        } catch (BusinessException exception) {
            status = ExecutionStatus.FAILED;
            errors.add(toError(exception, executionPlan == null ? request.getTraceRefs() : executionPlan.getTraceRefs()));
        } catch (RuntimeException exception) {
            status = ExecutionStatus.FAILED;
            errors.add(ExecutionError.builder()
                    .errorId("execution-error-" + UUID.randomUUID())
                    .errorCode(ErrorCode.EXECUTION_ADAPTER_FAILED.getErrorCode())
                    .message(exception.getMessage())
                    .stage("STRUCTURE_VALIDATION")
                    .recoverable(true)
                    .traceRefs(executionPlan == null ? request.getTraceRefs() : executionPlan.getTraceRefs())
                    .build());
        }
        LocalDateTime endTime = LocalDateTime.now();
        return ExecutionReport.builder()
                .executionId("execution-" + UUID.randomUUID())
                .taskId(request.getTaskId())
                .planId(executionPlan == null ? null : executionPlan.getPlanId())
                .configSetId(executionPlan == null ? null : executionPlan.getConfigSetId())
                .executionVersion(request.getExecutionVersion())
                .environmentType(ExecutionEnvironmentType.STRUCTURE_VALIDATION)
                .executionPlan(executionPlan)
                .runtimeState(runtimeState(startTime, endTime, status))
                .testResults(testResults(executionPlan, status))
                .errors(errors)
                .overallStatus(status)
                .traceRefs(executionPlan == null ? request.getTraceRefs() : executionPlan.getTraceRefs())
                .createTime(startTime)
                .startTime(startTime)
                .endTime(endTime)
                .updateTime(endTime)
                .build();
    }

    private void validateRequestEnvelope(ExecutionRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionRequest must not be null");
        }
        if (isBlank(request.getTaskId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionRequest.taskId is required");
        }
        if (request.getSelectedMode() != null && request.getSelectedMode() != ExecutionMode.STRUCTURE_VALIDATION) {
            throw new BusinessException(
                    ErrorCode.EXECUTION_ADAPTER_NOT_FOUND,
                    "StructureValidationExecutionAdapter does not support mode=" + request.getSelectedMode());
        }
        if (request.getTargetEnvironment() != null
                && request.getTargetEnvironment() != ExecutionEnvironmentType.STRUCTURE_VALIDATION) {
            throw new BusinessException(
                    ErrorCode.EXECUTION_ADAPTER_NOT_FOUND,
                    "StructureValidationExecutionAdapter does not support environment="
                            + request.getTargetEnvironment());
        }
    }

    private void validateExecutionPlan(ExecutionPlan executionPlan) {
        if (executionPlan == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan must not be null");
        }
        if (isBlank(executionPlan.getTaskId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan.taskId is required");
        }
        if (isBlank(executionPlan.getPlanId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan.planId is required");
        }
        if (isBlank(executionPlan.getConfigSetId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan.configSetId is required");
        }
        if (executionPlan.getTopology() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan.topology is required");
        }
        if (executionPlan.getTargetEnvironment() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan.targetEnvironment is required");
        }
        if (executionPlan.getExecutionMode() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan.executionMode is required");
        }
    }

    private RuntimeState runtimeState(LocalDateTime startedAt, LocalDateTime endedAt, ExecutionStatus status) {
        return RuntimeState.builder()
                .executorId(ADAPTER_ID)
                .executorEndpoint("not-applicable")
                .ryuControllerStatus("not-started")
                .mininetStatus("not-started")
                .environmentStatus(status == ExecutionStatus.SUCCESS ? "STRUCTURE_VALIDATED" : "STRUCTURE_INVALID")
                .logsSummary("Structure validation dry-run only; no Docker, Mininet, Ryu, network, or shell command was executed.")
                .startedAt(startedAt)
                .endedAt(endedAt)
                .build();
    }

    private List<TestResult> testResults(ExecutionPlan executionPlan, ExecutionStatus status) {
        List<TestResult> results = new ArrayList<>();
        if (executionPlan == null || executionPlan.getTestCommands() == null) {
            return results;
        }
        for (TestCommand testCommand : executionPlan.getTestCommands()) {
            results.add(TestResult.builder()
                    .testId(testCommand.getTestId())
                    .testType(testCommand.getTestType())
                    .status(status == ExecutionStatus.SUCCESS ? TestResultStatus.UNKNOWN : TestResultStatus.FAILED)
                    .sourceNode(testCommand.getSourceNode())
                    .targetNode(testCommand.getTargetNode())
                    .expectedResult(testCommand.getExpectedResult())
                    .actualResult(status == ExecutionStatus.SUCCESS
                            ? "dry-run descriptor validated; real test was not executed"
                            : "dry-run descriptor validation failed; real test was not executed")
                    .logsSummary("Dry-run result only. This is not a real ping, traceroute, iperf, flow table, or controller-state result.")
                    .traceRefs(testCommand.getTraceRefs())
                    .build());
        }
        return results;
    }

    private ExecutionError toError(BusinessException exception, TraceRefs traceRefs) {
        return ExecutionError.builder()
                .errorId("execution-error-" + UUID.randomUUID())
                .errorCode(exception.getErrorCode())
                .message(exception.getMessage())
                .stage("STRUCTURE_VALIDATION")
                .recoverable(true)
                .traceRefs(traceRefs)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
