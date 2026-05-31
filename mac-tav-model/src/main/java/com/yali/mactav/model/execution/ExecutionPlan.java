package com.yali.mactav.model.execution;

import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Controlled execution plan derived from NetworkPlan and ConfigSet.
 *
 * <p>The plan carries structured actions and topology references only. It must
 * not use arbitrary shell text as the execution contract.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlan {

    private String executionPlanId;

    private String planId;

    private String configSetId;

    private String taskId;

    private ExecutionEnvironmentType targetEnvironment;

    private ExecutionMode executionMode;

    private Topology topology;

    private String topologyScriptRef;

    @Builder.Default
    private List<ExecutionAction> actions = new ArrayList<>();

    @Builder.Default
    private List<ExecutionAction> cleanupActions = new ArrayList<>();

    @Builder.Default
    private List<FlowRule> flowRules = new ArrayList<>();

    @Builder.Default
    private List<TestCommand> testCommands = new ArrayList<>();

    private TraceRefs traceRefs;
}
