package com.yali.mactav.orchestrator.service;

import com.yali.mactav.model.enums.ArtifactType;

/**
 * Orchestrator facade for controlled artifact current-pointer switching.
 */
public interface ArtifactVersionSwitchService {

    ArtifactVersionSwitchResult switchCurrentArtifact(
            String taskId,
            ArtifactType artifactType,
            String targetArtifactId,
            String reason,
            String actor);
}
