package com.yali.mactav.execution.safety;

import com.yali.mactav.model.execution.ExecutionPlan;

/**
 * Entry point for execution safety validation.
 */
public class ExecutionSafetyPolicy {

    private final ExecutionActionValidator actionValidator;

    public ExecutionSafetyPolicy() {
        this(new ExecutionActionValidator());
    }

    public ExecutionSafetyPolicy(ExecutionActionValidator actionValidator) {
        this.actionValidator = actionValidator;
    }

    public void validate(ExecutionPlan executionPlan) {
        actionValidator.validatePlan(executionPlan);
    }
}
