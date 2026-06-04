package com.yali.mactav.orchestrator.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.modelcore.service.NetworkArtifactService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;

/**
 * Default artifact switch facade that validates intent and delegates writes to Model Core.
 */
public class DefaultArtifactVersionSwitchService implements ArtifactVersionSwitchService {

    private final NetworkWorkspaceService workspaceService;
    private final NetworkArtifactService artifactService;

    public DefaultArtifactVersionSwitchService(
            NetworkWorkspaceService workspaceService,
            NetworkArtifactService artifactService) {
        this.workspaceService = workspaceService;
        this.artifactService = artifactService;
    }

    @Override
    public ArtifactVersionSwitchResult switchCurrentArtifact(
            String taskId,
            ArtifactType artifactType,
            String targetArtifactId,
            String reason,
            String actor) {
        if (artifactType == null) {
            throw new BusinessException(ErrorCode.ARTIFACT_INVALID, "artifactType must not be null");
        }
        NetworkWorkspace before = workspaceService.getWorkspaceOrThrow(taskId);
        String fromArtifactId = before.getCurrentArtifactRefs() == null
                ? null
                : before.getCurrentArtifactRefs().get(artifactType);
        NetworkArtifact target = artifactService.findByArtifactId(targetArtifactId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ARTIFACT_NOT_FOUND,
                        "Artifact not found: " + targetArtifactId));
        if (!taskId.equals(target.getTaskId())) {
            throw new BusinessException(ErrorCode.ARTIFACT_NOT_FOUND, "Artifact not found: " + targetArtifactId);
        }
        if (target.getArtifactType() != artifactType) {
            throw new BusinessException(
                    ErrorCode.ARTIFACT_INVALID,
                    "Artifact type mismatch: expected " + artifactType + ", actual " + target.getArtifactType());
        }
        workspaceService.switchCurrentArtifact(taskId, artifactType, targetArtifactId, reason, actor);
        return new ArtifactVersionSwitchResult(
                taskId,
                artifactType,
                fromArtifactId,
                targetArtifactId,
                target.getVersion(),
                target.getStage(),
                reason,
                actor);
    }
}
