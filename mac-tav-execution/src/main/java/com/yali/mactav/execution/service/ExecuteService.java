package com.yali.mactav.execution.service;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.plan.NetworkPlan;

public interface ExecuteService {

    ExecutionReport execute(NetworkPlan plan, ConfigSet configSet);
}
