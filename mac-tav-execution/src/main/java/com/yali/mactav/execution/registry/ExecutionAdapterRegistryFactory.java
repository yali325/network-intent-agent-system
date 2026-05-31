package com.yali.mactav.execution.registry;

import com.yali.mactav.execution.adapter.StructureValidationExecutionAdapter;
import java.util.List;

/**
 * Creates execution adapter registries for local Java-side execution boundaries.
 */
public final class ExecutionAdapterRegistryFactory {

    private ExecutionAdapterRegistryFactory() {
    }

    public static ExecutionAdapterRegistry structureValidationRegistry() {
        return new ExecutionAdapterRegistry(List.of(new StructureValidationExecutionAdapter()));
    }
}
