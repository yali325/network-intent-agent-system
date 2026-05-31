package com.yali.mactav.execution.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.execution.adapter.ExecutionAdapter;
import com.yali.mactav.execution.model.ExecutionRequest;
import com.yali.mactav.model.execution.ExecutionEnvironmentType;
import com.yali.mactav.model.execution.ExecutionMode;
import com.yali.mactav.model.execution.ExecutionReport;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests adapter selection without invoking any real execution environment.
 */
class ExecutionAdapterRegistryTest {

    @Test
    void selectsAdapterByEnvironmentAndMode() {
        TestExecutionAdapter adapter = new TestExecutionAdapter(
                "structure-validation",
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                ExecutionMode.STRUCTURE_VALIDATION);

        ExecutionAdapterRegistry registry = new ExecutionAdapterRegistry(List.of(adapter));

        assertSame(adapter, registry.getRequired(
                ExecutionEnvironmentType.STRUCTURE_VALIDATION,
                ExecutionMode.STRUCTURE_VALIDATION));
        assertTrue(registry.find(ExecutionEnvironmentType.MININET_RYU, ExecutionMode.MININET_RYU).isEmpty());
    }

    @Test
    void throwsWhenAdapterNotFound() {
        ExecutionAdapterRegistry registry = new ExecutionAdapterRegistry(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> registry.getRequired(
                ExecutionEnvironmentType.MININET_RYU,
                ExecutionMode.MININET_RYU));

        assertEquals("EXECUTION_ADAPTER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void rejectsDuplicateAdapterRegistration() {
        TestExecutionAdapter first = new TestExecutionAdapter(
                "first",
                ExecutionEnvironmentType.MININET_RYU,
                ExecutionMode.MININET_RYU);
        TestExecutionAdapter second = new TestExecutionAdapter(
                "second",
                ExecutionEnvironmentType.MININET_RYU,
                ExecutionMode.MININET_RYU);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> new ExecutionAdapterRegistry(List.of(first, second)));

        assertEquals("EXECUTION_ADAPTER_FAILED", exception.getErrorCode());
    }

    private record TestExecutionAdapter(
            String adapterId,
            ExecutionEnvironmentType environmentType,
            ExecutionMode mode) implements ExecutionAdapter {

        @Override
        public ExecutionReport execute(ExecutionRequest request) {
            throw new UnsupportedOperationException("Registry tests do not execute adapters.");
        }
    }
}
