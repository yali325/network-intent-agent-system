package com.yali.mactav.agent.core.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Small validation result object shared by agent-core validators.
 *
 * <p>It intentionally carries messages only; it is not a workflow state object
 * and should not replace BusinessException or stage DTO status fields.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;

    @Builder.Default
    private List<String> messages = new ArrayList<>();

    public static ValidationResult ok() {
        return ValidationResult.builder().valid(true).build();
    }

    public static ValidationResult fail(String message) {
        ValidationResult result = ValidationResult.builder().valid(false).build();
        result.addMessage(message);
        return result;
    }

    public static ValidationResult fail(Collection<String> messages) {
        ValidationResult result = ValidationResult.builder().valid(false).build();
        if (messages != null) {
            result.getMessages().addAll(messages);
        }
        return result;
    }

    public void addMessage(String message) {
        if (message != null && !message.isBlank()) {
            messages.add(message);
        }
    }
}
