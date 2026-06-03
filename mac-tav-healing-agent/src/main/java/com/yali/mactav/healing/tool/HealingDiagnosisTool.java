package com.yali.mactav.healing.tool;

import com.yali.mactav.healing.request.HealingAgentRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight diagnostic helper for HealingAgent prompt grounding.
 */
public class HealingDiagnosisTool {

    public List<String> summarizeFailedValidationItems(HealingAgentRequest request) {
        if (request == null || request.getFailedValidationItemIds() == null) {
            return List.of();
        }
        return new ArrayList<>(request.getFailedValidationItemIds());
    }
}
