package com.yali.mactav.model.workspace;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.verification.ValidationReport;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkWorkspace {

    private NetworkTask task;
    private Integer currentIntentVersion;
    private Integer currentPlanVersion;
    private Integer currentConfigVersion;
    private Integer currentExecutionVersion;
    private Integer currentValidationVersion;
    private NetworkIntent intent;
    private NetworkPlan plan;
    private ConfigSet configSet;
    private ExecutionReport executionReport;
    private ValidationReport validationReport;
    private List<AgentStepLog> agentLogs;
}
