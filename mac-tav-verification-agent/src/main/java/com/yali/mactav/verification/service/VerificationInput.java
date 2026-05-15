package com.yali.mactav.verification.service;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;

public class VerificationInput {

    private final NetworkIntent intent;
    private final NetworkPlan plan;
    private final ConfigSet configSet;
    private final ExecutionReport executionReport;

    public VerificationInput(NetworkIntent intent, NetworkPlan plan, ConfigSet configSet,
                             ExecutionReport executionReport) {
        this.intent = intent;
        this.plan = plan;
        this.configSet = configSet;
        this.executionReport = executionReport;
    }

    public NetworkIntent getIntent() {
        return intent;
    }

    public NetworkPlan getPlan() {
        return plan;
    }

    public ConfigSet getConfigSet() {
        return configSet;
    }

    public ExecutionReport getExecutionReport() {
        return executionReport;
    }
}
