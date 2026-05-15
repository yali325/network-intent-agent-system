package com.yali.mactav.verification.impl;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.verification.agent.MockVerificationAgent;
import com.yali.mactav.verification.agent.VerificationAgent;
import com.yali.mactav.verification.service.VerificationInput;
import com.yali.mactav.verification.service.VerificationService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class VerificationServiceImpl implements VerificationService {

    private final VerificationAgent verificationAgent;

    public VerificationServiceImpl() {
        this(new MockVerificationAgent());
    }

    public VerificationServiceImpl(VerificationAgent verificationAgent) {
        this.verificationAgent = verificationAgent;
    }

    @Override
    public ValidationReport verify(NetworkIntent intent, NetworkPlan plan, ConfigSet configSet,
                                   ExecutionReport executionReport) {
        AgentContext context = AgentContext.of(intent == null ? null : intent.getTaskId(), null);
        AgentResult<ValidationReport> result = verificationAgent.execute(
                context,
                new VerificationInput(intent, plan, configSet, executionReport)
        );
        if (!result.isSuccess()) {
            throw new IllegalStateException(result.getMessage());
        }
        return result.getData();
    }
}
