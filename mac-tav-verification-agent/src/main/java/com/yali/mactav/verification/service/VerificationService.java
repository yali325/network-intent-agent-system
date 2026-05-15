package com.yali.mactav.verification.service;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.verification.ValidationReport;

public interface VerificationService {

    ValidationReport verify(NetworkIntent intent, NetworkPlan plan, ConfigSet configSet, ExecutionReport executionReport);
}
