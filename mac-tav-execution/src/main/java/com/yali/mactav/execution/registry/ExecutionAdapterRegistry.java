package com.yali.mactav.execution.registry;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.adapter.ExecutionAdapter;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory selector for execution adapters by environment and mode.
 *
 * <p>The registry only selects adapters. It does not execute work, write
 * Workspace state, manage task status, or create reports.</p>
 */
public class ExecutionAdapterRegistry {

    private final Map<ExecutionEnvironmentType, Map<ExecutionMode, ExecutionAdapter>> adapters;

    public ExecutionAdapterRegistry(List<ExecutionAdapter> adapters) {
        this.adapters = indexAdapters(adapters);
    }

    public Optional<ExecutionAdapter> find(ExecutionEnvironmentType environmentType, ExecutionMode mode) {
        if (environmentType == null || mode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(adapters.getOrDefault(environmentType, Collections.emptyMap()).get(mode));
    }

    public ExecutionAdapter getRequired(ExecutionEnvironmentType environmentType, ExecutionMode mode) {
        return find(environmentType, mode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.EXECUTION_ADAPTER_NOT_FOUND,
                        "Execution adapter not found for environmentType=" + environmentType + ", mode=" + mode));
    }

    public List<ExecutionAdapter> adapters() {
        List<ExecutionAdapter> all = new ArrayList<>();
        adapters.values().forEach(byMode -> all.addAll(byMode.values()));
        return Collections.unmodifiableList(all);
    }

    private Map<ExecutionEnvironmentType, Map<ExecutionMode, ExecutionAdapter>> indexAdapters(
            List<ExecutionAdapter> sourceAdapters) {
        Map<ExecutionEnvironmentType, Map<ExecutionMode, ExecutionAdapter>> result =
                new EnumMap<>(ExecutionEnvironmentType.class);
        if (sourceAdapters == null) {
            return result;
        }
        for (ExecutionAdapter adapter : sourceAdapters) {
            if (adapter == null || adapter.environmentType() == null || adapter.mode() == null) {
                continue;
            }
            result.computeIfAbsent(adapter.environmentType(), ignored -> new EnumMap<>(ExecutionMode.class))
                    .merge(adapter.mode(), adapter, this::rejectDuplicate);
        }
        return result;
    }

    private ExecutionAdapter rejectDuplicate(ExecutionAdapter first, ExecutionAdapter second) {
        throw new BusinessException(
                ErrorCode.EXECUTION_ADAPTER_FAILED,
                "Duplicate execution adapter registration for environmentType=" + first.environmentType()
                        + ", mode=" + first.mode()
                        + ", firstAdapter=" + safeAdapterId(first)
                        + ", secondAdapter=" + safeAdapterId(second));
    }

    private String safeAdapterId(ExecutionAdapter adapter) {
        return Objects.toString(adapter.adapterId(), "<blank>");
    }
}
