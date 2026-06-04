package com.yali.mactav.modelcore.query;

import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;

/**
 * Filter object for workspace event history queries.
 */
public record WorkspaceEventQuery(
        WorkflowStage stage,
        String eventType,
        LocalDateTime from,
        LocalDateTime to,
        int page,
        int size) {
}
