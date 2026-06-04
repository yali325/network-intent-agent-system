package com.yali.mactav.orchestrator.service;

import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;

/**
 * Summary returned after switching the current workspace artifact pointer.
 */
public record ArtifactVersionSwitchResult(
        String taskId,
        ArtifactType artifactType,
        String fromArtifactId,
        String toArtifactId,
        Integer targetVersion,
        WorkflowStage stage,
        String reason,
        String actor) {
}
