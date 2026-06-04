package com.yali.mactav.orchestrator.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.query.ArtifactQuery;
import com.yali.mactav.modelcore.query.WorkspaceChangeQuery;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
import com.yali.mactav.modelcore.service.NetworkArtifactService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.modelcore.service.WorkspaceChangeRecordService;
import com.yali.mactav.modelcore.service.WorkspaceEventService;

/**
 * Default read facade that exposes Model Core state through Orchestrator.
 */
public class DefaultWorkflowQueryService implements WorkflowQueryService {

    private final NetworkWorkspaceService workspaceService;
    private final NetworkArtifactService artifactService;
    private final WorkspaceEventService eventService;
    private final WorkspaceChangeRecordService changeRecordService;

    public DefaultWorkflowQueryService(NetworkWorkspaceService workspaceService,
                                       NetworkArtifactService artifactService,
                                       WorkspaceEventService eventService,
                                       WorkspaceChangeRecordService changeRecordService) {
        this.workspaceService = workspaceService;
        this.artifactService = artifactService;
        this.eventService = eventService;
        this.changeRecordService = changeRecordService;
    }

    @Override
    public PageResult<NetworkArtifact> listArtifacts(String taskId, ArtifactQuery query) {
        requireWorkspace(taskId);
        return artifactService.listArtifacts(taskId, query);
    }

    @Override
    public NetworkArtifact getArtifact(String taskId, String artifactId) {
        requireWorkspace(taskId);
        NetworkArtifact artifact = artifactService.findByArtifactId(artifactId)
                .orElseThrow(() -> artifactNotFound(artifactId));
        if (!taskId.equals(artifact.getTaskId())) {
            throw artifactNotFound(artifactId);
        }
        return artifact;
    }

    @Override
    public NetworkArtifact getCurrentArtifact(String taskId, ArtifactType artifactType) {
        NetworkWorkspace workspace = requireWorkspace(taskId);
        String artifactId = workspace.getCurrentArtifactRefs() == null
                ? null
                : workspace.getCurrentArtifactRefs().get(artifactType);
        if (artifactId == null || artifactId.isBlank()) {
            throw artifactNotFound(String.valueOf(artifactType));
        }
        return getArtifact(taskId, artifactId);
    }

    @Override
    public PageResult<NetworkArtifact> listArtifactVersions(String taskId, String artifactId, int page, int size) {
        NetworkArtifact artifact = getArtifact(taskId, artifactId);
        return artifactService.listArtifacts(
                taskId,
                new ArtifactQuery(artifact.getArtifactType(), null, page, size));
    }

    @Override
    public ArtifactDiffResult diffArtifactVersions(String taskId,
                                                  String artifactId,
                                                  Integer fromVersion,
                                                  Integer toVersion) {
        NetworkArtifact base = getArtifact(taskId, artifactId);
        int targetVersion = toVersion == null ? base.getVersion() : toVersion;
        int sourceVersion = fromVersion == null ? targetVersion - 1 : fromVersion;
        if (sourceVersion <= 0 || targetVersion <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Artifact versions must be positive");
        }
        NetworkArtifact from = artifactService
                .findByTaskIdTypeVersion(taskId, base.getArtifactType(), sourceVersion)
                .orElseThrow(() -> artifactNotFound(base.getArtifactType() + " version " + sourceVersion));
        NetworkArtifact to = artifactService
                .findByTaskIdTypeVersion(taskId, base.getArtifactType(), targetVersion)
                .orElseThrow(() -> artifactNotFound(base.getArtifactType() + " version " + targetVersion));
        return new ArtifactDiffResult(from, to);
    }

    @Override
    public PageResult<WorkspaceEvent> listTimeline(String taskId, WorkspaceEventQuery query) {
        requireWorkspace(taskId);
        return eventService.listEvents(taskId, query);
    }

    @Override
    public PageResult<WorkspaceChangeRecord> listChanges(String taskId, WorkspaceChangeQuery query) {
        requireWorkspace(taskId);
        return changeRecordService.listChanges(taskId, query);
    }

    @Override
    public PageResult<WorkspaceEvent> listEventHistory(String taskId, WorkspaceEventQuery query) {
        requireWorkspace(taskId);
        return eventService.listEvents(taskId, query);
    }

    private NetworkWorkspace requireWorkspace(String taskId) {
        return workspaceService.getWorkspaceOrThrow(taskId);
    }

    private BusinessException artifactNotFound(String artifactId) {
        return new BusinessException(ErrorCode.ARTIFACT_NOT_FOUND, "Artifact not found: " + artifactId);
    }
}
