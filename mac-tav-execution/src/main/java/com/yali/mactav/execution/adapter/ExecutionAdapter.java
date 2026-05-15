package com.yali.mactav.execution.adapter;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.plan.NetworkPlan;

public interface ExecutionAdapter {

    ExecutionReport adapt(NetworkPlan plan, ConfigSet configSet);
}
