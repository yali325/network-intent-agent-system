package com.yali.mactav.orchestrator.service;

import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.query.ArtifactQuery;
import com.yali.mactav.modelcore.query.WorkspaceChangeQuery;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;

/**
 * Orchestrator read facade for artifact and workspace history APIs.
 */
public interface WorkflowQueryService {

    PageResult<NetworkArtifact> listArtifacts(String taskId, ArtifactQuery query);

    NetworkArtifact getArtifact(String taskId, String artifactId);

    NetworkArtifact getCurrentArtifact(String taskId, ArtifactType artifactType);

    PageResult<NetworkArtifact> listArtifactVersions(String taskId, String artifactId, int page, int size);

    ArtifactDiffResult diffArtifactVersions(String taskId, String artifactId, Integer fromVersion, Integer toVersion);

    PageResult<WorkspaceEvent> listTimeline(String taskId, WorkspaceEventQuery query);

    PageResult<WorkspaceChangeRecord> listChanges(String taskId, WorkspaceChangeQuery query);

    PageResult<WorkspaceEvent> listEventHistory(String taskId, WorkspaceEventQuery query);
}
