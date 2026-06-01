package com.yali.mactav.execution.registry;

import com.yali.mactav.execution.adapter.ExecutionAdapter;
import com.yali.mactav.execution.adapter.MininetRyuExecutionAdapter;
import com.yali.mactav.execution.adapter.StructureValidationExecutionAdapter;
import com.yali.mactav.execution.client.MininetRyuExecutorClient;
import com.yali.mactav.execution.config.ExecutionProperties;
import java.util.ArrayList;
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

    public static ExecutionAdapterRegistry mininetRyuRegistry(ExecutionProperties properties) {
        return new ExecutionAdapterRegistry(List.of(
                new MininetRyuExecutionAdapter(new MininetRyuExecutorClient(properties))));
    }

    public static ExecutionAdapterRegistry defaultRegistry(ExecutionProperties properties) {
        List<ExecutionAdapter> adapters = new ArrayList<>();
        adapters.add(new StructureValidationExecutionAdapter());
        if (properties != null
                && properties.getMininetRyu() != null
                && properties.getMininetRyu().isEnabled()) {
            adapters.add(new MininetRyuExecutionAdapter(new MininetRyuExecutorClient(properties)));
        }
        return new ExecutionAdapterRegistry(adapters);
    }
}
