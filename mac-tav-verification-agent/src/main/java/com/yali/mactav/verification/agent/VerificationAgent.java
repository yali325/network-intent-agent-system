package com.yali.mactav.verification.agent;

import com.yali.mactav.agent.core.agent.BaseAgent;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.verification.service.VerificationInput;

public interface VerificationAgent extends BaseAgent<VerificationInput, ValidationReport> {
}
