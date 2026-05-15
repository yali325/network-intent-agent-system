package com.yali.mactav.execution.impl;

import com.yali.mactav.execution.adapter.DryRunExecutionAdapter;
import com.yali.mactav.execution.adapter.ExecutionAdapter;
import com.yali.mactav.execution.service.ExecuteService;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.plan.NetworkPlan;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ExecuteServiceImpl implements ExecuteService {

    private final ExecutionAdapter executionAdapter;

    public ExecuteServiceImpl() {
        this(new DryRunExecutionAdapter());
    }

    public ExecuteServiceImpl(ExecutionAdapter executionAdapter) {
        this.executionAdapter = executionAdapter;
    }

    @Override
    public ExecutionReport execute(NetworkPlan plan, ConfigSet configSet) {
        return executionAdapter.adapt(plan, configSet);
    }
}
