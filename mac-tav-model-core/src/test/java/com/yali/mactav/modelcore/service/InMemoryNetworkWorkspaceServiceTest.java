package com.yali.mactav.modelcore.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.TaskStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.healing.RepairPlan;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.workspace.NetworkArtifact;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.modelcore.ModelCoreTestFixture;
import com.yali.mactav.modelcore.event.WorkspaceEventTypes;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryNetworkWorkspaceServiceTest {

    @Test
    void createWorkspaceShouldCreateInitialWorkspace() {
        ModelCoreTestFixture.Services services = ModelCoreTestFixture.services();

        NetworkWorkspace workspace = services.workspaceService().createWorkspace(task("task-001"));

        assertEquals("task-001", workspace.getTask().getTaskId());
        assertEquals(TaskStatus.CREATED, workspace.getWorkspaceStatus());
        assertEquals(WorkflowStage.INTENT, workspace.getTask().getCurrentStage());
        assertEquals(1, workspace.getEvents().size());
        assertEquals(WorkspaceEventTypes.TASK_CREATED, workspace.getEvents().get(0).getEventType());
    }

    @Test
    void saveStageArtifactShouldSaveIntentArtifactAndUpdateCurrentRefs() {
        ModelCoreTestFixture.Services services = ModelCoreTestFixture.services();
        services.workspaceService().createWorkspace(task("task-002"));
        NetworkIntent intent = intent("task-002", "Build a low latency network");

        NetworkArtifact artifact = services.workspaceService().saveStageArtifact(
                "task-002",
                ArtifactType.NETWORK_INTENT,
                WorkflowStage.INTENT,
                intent,
                "intent summary",
                "IntentAgent",
                TraceRefs.builder().build());

        NetworkWorkspace workspace = services.workspaceService().getWorkspaceOrThrow("task-002");
        assertEquals(artifact.getArtifactId(), workspace.getCurrentArtifactRefs().get(ArtifactType.NETWORK_INTENT));
        assertEquals(1, workspace.getCurrentIntentVersion());
        assertSame(intent, workspace.getCurrentIntent());
        assertEquals(2, workspace.getEvents().size());
        assertEquals(WorkspaceEventTypes.ARTIFACT_GENERATED, workspace.getEvents().get(1).getEventType());
    }

    @Test
    void saveStageArtifactShouldSyncRepairPlanAndAppendRepairEvent() {
        ModelCoreTestFixture.Services services = ModelCoreTestFixture.services();
        services.workspaceService().createWorkspace(task("task-repair-001"));
        RepairPlan repairPlan = RepairPlan.builder()
                .taskId("task-repair-001")
                .validationVersion(1)
                .repairVersion(1)
                .overallRepairStrategy("Propose a minimal configuration correction.")
                .stageStatus(StageStatus.SUCCESS)
                .requiresUserConfirmation(true)
                .build();

        NetworkArtifact artifact = services.workspaceService().saveStageArtifact(
                "task-repair-001",
                ArtifactType.REPAIR_PLAN,
                WorkflowStage.HEALING,
                repairPlan,
                "repair summary",
                "HealingAgent",
                TraceRefs.builder().build());

        NetworkWorkspace workspace = services.workspaceService().getWorkspaceOrThrow("task-repair-001");
        assertEquals(artifact.getArtifactId(), workspace.getCurrentArtifactRefs().get(ArtifactType.REPAIR_PLAN));
        assertEquals(1, workspace.getCurrentRepairVersion());
        assertSame(repairPlan, workspace.getCurrentRepairPlan());
        assertEquals(3, workspace.getEvents().size());
        assertEquals(WorkspaceEventTypes.ARTIFACT_GENERATED, workspace.getEvents().get(1).getEventType());
        assertEquals(WorkspaceEventTypes.REPAIR_PROPOSED, workspace.getEvents().get(2).getEventType());
    }

    @Test
    void saveStageArtifactShouldIncrementVersionsAndKeepHistory() {
        ModelCoreTestFixture.Services services = ModelCoreTestFixture.services();
        services.workspaceService().createWorkspace(task("task-003"));

        NetworkArtifact first = services.workspaceService().saveStageArtifact(
                "task-003",
                ArtifactType.NETWORK_INTENT,
                WorkflowStage.INTENT,
                intent("task-003", "Initial intent"),
                "v1",
                "IntentAgent",
                TraceRefs.builder().build());
        NetworkArtifact second = services.workspaceService().saveStageArtifact(
                "task-003",
                ArtifactType.NETWORK_INTENT,
                WorkflowStage.INTENT,
                intent("task-003", "Refined intent"),
                "v2",
                "IntentAgent",
                TraceRefs.builder().build());

        NetworkWorkspace workspace = services.workspaceService().getWorkspaceOrThrow("task-003");
        List<NetworkArtifact> artifacts = services.artifactService()
                .listByTaskIdAndType("task-003", ArtifactType.NETWORK_INTENT);
        assertEquals(1, first.getVersion());
        assertEquals(2, second.getVersion());
        assertEquals(2, workspace.getCurrentIntentVersion());
        assertEquals(second.getArtifactId(), workspace.getCurrentArtifactRefs().get(ArtifactType.NETWORK_INTENT));
        assertEquals(2, artifacts.size());
        assertNotNull(services.artifactService().findByArtifactId(first.getArtifactId()).orElse(null));
    }

    @Test
    void switchCurrentArtifactShouldOnlyMoveCurrentPointerAndRecordAudit() {
        ModelCoreTestFixture.Services services = ModelCoreTestFixture.services();
        services.workspaceService().createWorkspace(task("task-switch-001"));
        NetworkArtifact first = services.workspaceService().saveStageArtifact(
                "task-switch-001",
                ArtifactType.NETWORK_INTENT,
                WorkflowStage.INTENT,
                intent("task-switch-001", "Initial intent"),
                "v1",
                "IntentAgent",
                TraceRefs.builder().build());
        NetworkArtifact second = services.workspaceService().saveStageArtifact(
                "task-switch-001",
                ArtifactType.NETWORK_INTENT,
                WorkflowStage.INTENT,
                intent("task-switch-001", "Refined intent"),
                "v2",
                "IntentAgent",
                TraceRefs.builder().build());
        String originalPayloadJson = first.getPayloadJson();

        NetworkWorkspace workspace = services.workspaceService().switchCurrentArtifact(
                "task-switch-001",
                ArtifactType.NETWORK_INTENT,
                first.getArtifactId(),
                "manual switch in test",
                "unit-test");

        assertEquals(first.getArtifactId(), workspace.getCurrentArtifactRefs().get(ArtifactType.NETWORK_INTENT));
        assertEquals(1, workspace.getCurrentIntentVersion());
        assertEquals(1, services.changeRecordService().listChanges("task-switch-001").size());
        assertEquals("VERSION_SWITCH", services.changeRecordService()
                .listChanges("task-switch-001").get(0).getChangeType());
        assertEquals(second.getArtifactId(), services.changeRecordService()
                .listChanges("task-switch-001").get(0).getFromArtifactId());
        assertEquals(WorkspaceEventTypes.ARTIFACT_VERSION_SWITCHED, workspace.getEvents()
                .get(workspace.getEvents().size() - 1).getEventType());
        assertEquals(originalPayloadJson, services.artifactService()
                .findByArtifactId(first.getArtifactId()).orElseThrow().getPayloadJson());
    }

    @Test
    void getWorkspaceOrThrowShouldThrowWhenMissing() {
        ModelCoreTestFixture.Services services = ModelCoreTestFixture.services();

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> services.workspaceService().getWorkspaceOrThrow("missing-task"));

        assertEquals(ErrorCode.WORKSPACE_NOT_FOUND.getErrorCode(), exception.getErrorCode());
    }

    private NetworkTask task(String taskId) {
        return NetworkTask.builder()
                .taskId(taskId)
                .rawText("raw intent")
                .build();
    }

    private NetworkIntent intent(String taskId, String rawText) {
        return NetworkIntent.builder()
                .taskId(taskId)
                .intentVersion(1)
                .rawText(rawText)
                .stageStatus(StageStatus.SUCCESS)
                .build();
    }
}
