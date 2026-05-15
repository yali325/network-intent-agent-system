package com.yali.mactav.model.execution;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPlan {

    private String adapterType;
    private String topologyScript;
    private List<ExecutionCommand> hostCommands;
    private List<FlowRule> flowRules;
    private List<TestCommand> testCommands;
}
