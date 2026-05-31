package com.yali.mactav.execution.adapter;

import com.yali.mactav.execution.model.ExecutionRequest;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionReport;

/**
 * Boundary for controlled execution implementations.
 *
 * <p>Adapters execute structured {@link ExecutionRequest} payloads only. They
 * must not accept raw shell text or mutate Workspace state directly.</p>
 */
public interface ExecutionAdapter {

    String adapterId();

    ExecutionEnvironmentType environmentType();

    ExecutionMode mode();

    default boolean supports(ExecutionEnvironmentType environmentType, ExecutionMode mode) {
        return environmentType() == environmentType && mode() == mode;
    }

    ExecutionReport execute(ExecutionRequest request);
}
