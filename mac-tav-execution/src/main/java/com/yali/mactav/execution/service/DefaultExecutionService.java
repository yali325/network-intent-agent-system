package com.yali.mactav.execution.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.adapter.ExecutionAdapter;
import com.yali.mactav.execution.converter.NetworkExecutionPlanConverter;
import com.yali.mactav.execution.model.ExecutionRequest;
import com.yali.mactav.execution.registry.ExecutionAdapterRegistry;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.ExecutionReport;

/**
 * Default ExecutionService that converts inputs, selects an adapter, and returns an ExecutionReport.
 */
public class DefaultExecutionService implements ExecutionService {

    private final ExecutionAdapterRegistry adapterRegistry;

    private final NetworkExecutionPlanConverter converter;

    public DefaultExecutionService(ExecutionAdapterRegistry adapterRegistry) {
        this(adapterRegistry, new NetworkExecutionPlanConverter());
    }

    public DefaultExecutionService(
            ExecutionAdapterRegistry adapterRegistry,
            NetworkExecutionPlanConverter converter) {
        this.adapterRegistry = adapterRegistry;
        this.converter = converter;
    }

    @Override
    public ExecutionReport execute(ExecutionServiceRequest request) {
        validate(request);
        ExecutionMode mode = resolveMode(request);
        ExecutionEnvironmentType environmentType = resolveEnvironment(request, mode);
        ExecutionPlan executionPlan = resolvePlan(request, mode, environmentType);
        ExecutionAdapter adapter = adapterRegistry.getRequired(environmentType, mode);
        return adapter.execute(new ExecutionRequest(
                request.getTaskId(),
                request.getNetworkPlan(),
                request.getConfigSet(),
                executionPlan,
                request.getExecutionVersion(),
                mode,
                environmentType,
                request.getTraceRefs(),
                request.getArtifactRefs()));
    }

    private void validate(ExecutionServiceRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionServiceRequest must not be null");
        }
        if (request.getTaskId() == null || request.getTaskId().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "ExecutionServiceRequest.taskId is required");
        }
    }

    private ExecutionMode resolveMode(ExecutionServiceRequest request) {
        if (request.getMode() != null) {
            return request.getMode();
        }
        if (request.getExecutionPlan() != null && request.getExecutionPlan().getExecutionMode() != null) {
            return request.getExecutionPlan().getExecutionMode();
        }
        return ExecutionMode.STRUCTURE_VALIDATION;
    }

    private ExecutionEnvironmentType resolveEnvironment(ExecutionServiceRequest request, ExecutionMode mode) {
        if (request.getEnvironmentType() != null) {
            return request.getEnvironmentType();
        }
        if (request.getExecutionPlan() != null && request.getExecutionPlan().getTargetEnvironment() != null) {
            return request.getExecutionPlan().getTargetEnvironment();
        }
        if (mode == ExecutionMode.MININET_RYU) {
            return ExecutionEnvironmentType.MININET_RYU;
        }
        return ExecutionEnvironmentType.STRUCTURE_VALIDATION;
    }

    private ExecutionPlan resolvePlan(
            ExecutionServiceRequest request,
            ExecutionMode mode,
            ExecutionEnvironmentType environmentType) {
        if (request.getExecutionPlan() != null) {
            return request.getExecutionPlan();
        }
        if (request.getNetworkPlan() == null || request.getConfigSet() == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "ExecutionService requires executionPlan or both networkPlan and configSet");
        }
        return converter.convert(
                request.getNetworkPlan(),
                request.getConfigSet(),
                request.getExecutionVersion(),
                mode,
                environmentType,
                request.getTraceRefs(),
                request.getArtifactRefs());
    }
}
