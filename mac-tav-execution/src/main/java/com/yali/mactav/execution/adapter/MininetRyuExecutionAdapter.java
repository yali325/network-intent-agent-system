package com.yali.mactav.execution.adapter;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.client.MininetRyuExecutorClient;
import com.yali.mactav.execution.client.dto.MininetRyuErrorResponse;
import com.yali.mactav.execution.client.dto.MininetRyuRunResponse;
import com.yali.mactav.execution.client.dto.MininetRyuRuntimeStateResponse;
import com.yali.mactav.execution.client.dto.MininetRyuTestResultResponse;
import com.yali.mactav.execution.converter.NetworkExecutionPlanConverter;
import com.yali.mactav.execution.model.ExecutionRequest;
import com.yali.mactav.execution.safety.ExecutionSafetyPolicy;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionError;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.execution.ExecutionStatus;
import com.yali.mactav.model.execution.RuntimeState;
import com.yali.mactav.model.execution.TestResult;
import com.yali.mactav.model.execution.TestResultStatus;
import com.yali.mactav.model.execution.TestResultType;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ExecutionAdapter that sends structured plans to the Python Mininet/Ryu executor.
 */
public class MininetRyuExecutionAdapter implements ExecutionAdapter {

    public static final String ADAPTER_ID = "mininet-ryu-execution-adapter";

    private final MininetRyuExecutorClient executorClient;

    private final ExecutionSafetyPolicy safetyPolicy;

    private final NetworkExecutionPlanConverter converter;

    public MininetRyuExecutionAdapter(MininetRyuExecutorClient executorClient) {
        this(executorClient, new ExecutionSafetyPolicy(), new NetworkExecutionPlanConverter());
    }

    public MininetRyuExecutionAdapter(
            MininetRyuExecutorClient executorClient,
            ExecutionSafetyPolicy safetyPolicy,
            NetworkExecutionPlanConverter converter) {
        this.executorClient = executorClient;
        this.safetyPolicy = safetyPolicy;
        this.converter = converter;
    }

    @Override
    public String adapterId() {
        return ADAPTER_ID;
    }

    @Override
    public ExecutionEnvironmentType environmentType() {
        return ExecutionEnvironmentType.MININET_RYU;
    }

    @Override
    public ExecutionMode mode() {
        return ExecutionMode.MININET_RYU;
    }

    @Override
    public ExecutionReport execute(ExecutionRequest request) {
        validateRequestEnvelope(request);
        LocalDateTime createTime = LocalDateTime.now();
        ExecutionPlan executionPlan = null;
        try {
            executionPlan = resolveExecutionPlan(request);
            validateExecutionPlan(executionPlan);
            safetyPolicy.validate(executionPlan);
            String executionId = "execution-" + UUID.randomUUID();
            MininetRyuRunResponse response = executorClient.run(executionId, executionPlan, request.getExecutionVersion());
            return toReport(request, executionPlan, response, createTime);
        } catch (BusinessException exception) {
            return failedReport(request, executionPlan, exception, createTime);
        } catch (RuntimeException exception) {
            return failedReport(
                    request,
                    executionPlan,
                    new BusinessException(
                            ErrorCode.EXECUTION_ADAPTER_FAILED,
                            "Mininet/Ryu execution adapter failed: " + exception.getClass().getSimpleName()),
                    createTime);
        }
    }

    private ExecutionPlan resolveExecutionPlan(ExecutionRequest request) {
        if (request.getExecutionPlan() != null) {
            return request.getExecutionPlan();
        }
        if (request.getNetworkPlan() != null && request.getConfigSet() != null) {
            return converter.convert(
                    request.getNetworkPlan(),
                    request.getConfigSet(),
                    request.getExecutionVersion(),
                    ExecutionMode.MININET_RYU,
                    ExecutionEnvironmentType.MININET_RYU,
                    request.getTraceRefs(),
                    request.getArtifactRefs());
        }
        throw new BusinessException(
                ErrorCode.PARAM_INVALID,
                "ExecutionRequest must include executionPlan or both networkPlan and configSet");
    }

    private void validateRequestEnvelope(ExecutionRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionRequest must not be null");
        }
        if (isBlank(request.getTaskId())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionRequest.taskId is required");
        }
        if (request.getSelectedMode() != null && request.getSelectedMode() != ExecutionMode.MININET_RYU) {
            throw new BusinessException(
                    ErrorCode.EXECUTION_ADAPTER_NOT_FOUND,
                    "MininetRyuExecutionAdapter does not support mode=" + request.getSelectedMode());
        }
        if (request.getTargetEnvironment() != null
                && request.getTargetEnvironment() != ExecutionEnvironmentType.MININET_RYU) {
            throw new BusinessException(
                    ErrorCode.EXECUTION_ADAPTER_NOT_FOUND,
                    "MininetRyuExecutionAdapter does not support environment=" + request.getTargetEnvironment());
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
        if (executionPlan.getTargetEnvironment() != ExecutionEnvironmentType.MININET_RYU) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan.targetEnvironment must be MININET_RYU");
        }
        if (executionPlan.getExecutionMode() != ExecutionMode.MININET_RYU) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionPlan.executionMode must be MININET_RYU");
        }
    }

    private ExecutionReport toReport(
            ExecutionRequest request,
            ExecutionPlan executionPlan,
            MininetRyuRunResponse response,
            LocalDateTime createTime) {
        LocalDateTime startTime = toLocalDateTime(response.startedAt(), createTime);
        LocalDateTime endTime = toLocalDateTime(response.endedAt(), LocalDateTime.now());
        return ExecutionReport.builder()
                .executionId(response.executionId())
                .taskId(executionPlan.getTaskId())
                .planId(executionPlan.getPlanId())
                .configSetId(executionPlan.getConfigSetId())
                .executionVersion(request.getExecutionVersion())
                .environmentType(ExecutionEnvironmentType.MININET_RYU)
                .executionPlan(executionPlan)
                .runtimeState(toRuntimeState(response.runtimeState(), response.flowStats()))
                .testResults(toTestResults(response.testResults()))
                .errors(toErrors(response.errors()))
                .overallStatus(toExecutionStatus(response.status()))
                .traceRefs(executionPlan.getTraceRefs())
                .createTime(createTime)
                .startTime(startTime)
                .endTime(endTime)
                .updateTime(endTime)
                .build();
    }

    private ExecutionReport failedReport(
            ExecutionRequest request,
            ExecutionPlan executionPlan,
            BusinessException exception,
            LocalDateTime createTime) {
        LocalDateTime endTime = LocalDateTime.now();
        TraceRefs traceRefs = executionPlan == null ? request.getTraceRefs() : executionPlan.getTraceRefs();
        return ExecutionReport.builder()
                .executionId("execution-" + UUID.randomUUID())
                .taskId(request.getTaskId())
                .planId(executionPlan == null ? null : executionPlan.getPlanId())
                .configSetId(executionPlan == null ? null : executionPlan.getConfigSetId())
                .executionVersion(request.getExecutionVersion())
                .environmentType(ExecutionEnvironmentType.MININET_RYU)
                .executionPlan(executionPlan)
                .runtimeState(RuntimeState.builder()
                        .executorId(ADAPTER_ID)
                        .executorEndpoint("configured-python-executor")
                        .ryuControllerStatus("unknown")
                        .mininetStatus("unknown")
                        .environmentStatus("FAILED_BEFORE_OR_DURING_EXECUTOR_CALL")
                        .logsSummary("Mininet/Ryu execution failed before producing a successful executor response.")
                        .startedAt(createTime)
                        .endedAt(endTime)
                        .build())
                .errors(List.of(toError(exception, traceRefs)))
                .overallStatus(ExecutionStatus.FAILED)
                .traceRefs(traceRefs)
                .createTime(createTime)
                .startTime(createTime)
                .endTime(endTime)
                .updateTime(endTime)
                .build();
    }

    private RuntimeState toRuntimeState(MininetRyuRuntimeStateResponse response, Map<String, Object> flowStats) {
        if (response == null) {
            return RuntimeState.builder()
                    .executorId(ADAPTER_ID)
                    .environmentStatus("UNKNOWN")
                    .flowState(flowStats == null ? Map.of() : flowStats)
                    .build();
        }
        return RuntimeState.builder()
                .executorId(response.executorId())
                .executorEndpoint(response.executorEndpoint())
                .ryuControllerStatus(response.ryuControllerStatus())
                .mininetStatus(response.mininetStatus())
                .environmentStatus(response.environmentStatus())
                .logsSummary(response.logsSummary())
                .flowState(flowStats == null ? Map.of() : flowStats)
                .startedAt(toLocalDateTime(response.startedAt(), null))
                .endedAt(toLocalDateTime(response.endedAt(), null))
                .build();
    }

    private List<TestResult> toTestResults(List<MininetRyuTestResultResponse> responses) {
        List<TestResult> results = new ArrayList<>();
        if (responses == null) {
            return results;
        }
        for (MininetRyuTestResultResponse response : responses) {
            results.add(TestResult.builder()
                    .testId(response.testId())
                    .testType(toTestResultType(response.testType()))
                    .status(toTestResultStatus(response.status()))
                    .sourceNode(firstNonBlank(response.sourceNode(), response.sourceNodeId()))
                    .targetNode(firstNonBlank(response.targetNode(), response.targetNodeId()))
                    .expectedResult(response.expectedResult())
                    .actualResult(firstNonBlank(response.actualResult(), response.resultSummary(), response.stdoutSummary()))
                    .metrics(response.metrics() == null ? Map.of() : response.metrics())
                    .logsSummary(firstNonBlank(response.logsSummary(), response.resultSummary(), response.stdoutSummary()))
                    .evidenceRefs(response.evidenceRefs() == null ? Map.of() : response.evidenceRefs())
                    .traceRefs(response.traceRefs())
                    .build());
        }
        return results;
    }

    private List<ExecutionError> toErrors(List<MininetRyuErrorResponse> responses) {
        List<ExecutionError> errors = new ArrayList<>();
        if (responses == null) {
            return errors;
        }
        for (MininetRyuErrorResponse response : responses) {
            errors.add(ExecutionError.builder()
                    .errorId("execution-error-" + UUID.randomUUID())
                    .errorCode(response.errorCode())
                    .message(response.message())
                    .stage(response.stage())
                    .actionId(response.actionId())
                    .relatedCommandId(firstNonBlank(response.testId(), response.actionId()))
                    .recoverable(response.recoverable())
                    .traceRefs(response.traceRefs())
                    .build());
        }
        return errors;
    }

    private ExecutionError toError(BusinessException exception, TraceRefs traceRefs) {
        return ExecutionError.builder()
                .errorId("execution-error-" + UUID.randomUUID())
                .errorCode(exception.getErrorCode())
                .message(exception.getMessage())
                .stage("MININET_RYU_ADAPTER")
                .recoverable(true)
                .traceRefs(traceRefs)
                .build();
    }

    private ExecutionStatus toExecutionStatus(String value) {
        if (value == null) {
            return ExecutionStatus.FAILED;
        }
        try {
            return ExecutionStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return ExecutionStatus.FAILED;
        }
    }

    private TestResultStatus toTestResultStatus(String value) {
        if (value == null) {
            return TestResultStatus.UNKNOWN;
        }
        try {
            return TestResultStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return TestResultStatus.UNKNOWN;
        }
    }

    private TestResultType toTestResultType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PING", "PING_TEST" -> TestResultType.PING;
            case "TRACEROUTE", "TRACEROUTE_TEST" -> TestResultType.TRACEROUTE;
            case "IPERF", "IPERF_TEST" -> TestResultType.IPERF;
            case "FLOW_TABLE", "RYU_FLOW_QUERY" -> TestResultType.FLOW_TABLE;
            case "CONTROLLER_STATE", "RYU_CONTROLLER_CHECK" -> TestResultType.CONTROLLER_STATE;
            case "TOPOLOGY_STATE", "TOPOLOGY_STATE_CHECK" -> TestResultType.TOPOLOGY_STATE;
            default -> TestResultType.TOPOLOGY_STATE;
        };
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value, LocalDateTime fallback) {
        return value == null ? fallback : value.toLocalDateTime();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
