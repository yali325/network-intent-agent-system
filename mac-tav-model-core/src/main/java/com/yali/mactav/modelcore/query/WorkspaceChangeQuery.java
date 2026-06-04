package com.yali.mactav.modelcore.query;

import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;

/**
 * Filter object for workspace change history queries.
 */
public record WorkspaceChangeQuery(
        WorkflowStage stage,
        String changeType,
        LocalDateTime from,
        LocalDateTime to,
        int page,
        int size) {
}
