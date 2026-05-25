package com.yali.mactav.model.execution;

import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlan {

    private String adapterType;

    private String topologyScript;

    @Builder.Default
    private List<ExecutionCommand> hostCommands = new ArrayList<>();

    @Builder.Default
    private List<FlowRule> flowRules = new ArrayList<>();

    @Builder.Default
    private List<TestCommand> testCommands = new ArrayList<>();

    @Builder.Default
    private List<ExecutionCommand> cleanupCommands = new ArrayList<>();

    private TraceRefs traceRefs;
}
