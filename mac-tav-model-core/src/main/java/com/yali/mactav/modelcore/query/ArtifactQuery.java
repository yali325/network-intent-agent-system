package com.yali.mactav.modelcore.query;

import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;

/**
 * Filter object for artifact history queries.
 */
public record ArtifactQuery(
        ArtifactType artifactType,
        WorkflowStage stage,
        int page,
        int size) {
}
