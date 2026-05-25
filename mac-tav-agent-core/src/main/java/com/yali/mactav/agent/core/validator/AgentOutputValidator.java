package com.yali.mactav.agent.core.validator;

/**
 * Validates a parsed agent DTO before it can advance to orchestration/model-core.
 *
 * <p>Concrete validators live in agent modules. This interface must stay
 * business-agnostic and must not write workspace state.</p>
 */
public interface AgentOutputValidator<D> {

    ValidationResult validate(D output);

    default D validateAndReturn(D output) {
        ValidationResult result = validate(output);
        if (result == null || !result.isValid()) {
            throw new AgentValidationException(result);
        }
        return output;
    }
}
