package com.yali.mactav.orchestrator.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.common.enums.TaskStatus;
import com.yali.mactav.common.enums.WorkflowStage;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.configuration.impl.ConfigurationServiceImpl;
import com.yali.mactav.configuration.service.ConfigurationService;
import com.yali.mactav.execution.impl.ExecuteServiceImpl;
import com.yali.mactav.execution.service.ExecuteService;
import com.yali.mactav.intent.impl.IntentServiceImpl;
import com.yali.mactav.intent.service.IntentService;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.AgentStepLog;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.modelcore.impl.NetworkWorkspaceServiceImpl;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceRepository;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.orchestrator.service.TaskOrchestratorService;
import com.yali.mactav.planning.impl.PlanningServiceImpl;
import com.yali.mactav.planning.service.PlanningService;
import com.yali.mactav.verification.impl.VerificationServiceImpl;
import com.yali.mactav.verification.service.VerificationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TaskOrchestratorServiceImplTest {

    private static final String RAW_TEXT = "Create an office and guest network with server access control";

    @Test
    void runDemoTaskCompletesAndReturnsFullWorkspace() {
        TaskOrchestratorService orchestrator = orchestrator(newWorkspaceService());

        NetworkWorkspace workspace = orchestrator.runDemoTask(RAW_TEXT);

        assertNotNull(workspace.getTask());
        assertNotNull(workspace.getIntent());
        assertNotNull(workspace.getPlan());
        assertNotNull(workspace.getConfigSet());
        assertNotNull(workspace.getExecutionReport());
        assertNotNull(workspace.getValidationReport());
        assertNotNull(workspace.getAgentLogs());
        assertFalse(workspace.getAgentLogs().isEmpty());
        assertEquals(TaskStatus.PASSED, workspace.getTask().getTaskStatus());
        assertEquals(WorkflowStage.VERIFICATION.name(), workspace.getTask().getCurrentStage());
        assertTrue(workspace.getAgentLogs().size() >= 5);
        assertCompletedStageLogs(workspace);
    }

    @Test
    void runDemoTaskCallsStagesInOrder() {
        List<String> calls = new ArrayList<>();
        IntentService intentService = (taskId, rawText) -> {
            calls.add("INTENT");
            return new IntentServiceImpl().parseIntent(taskId, rawText);
        };
        PlanningService planningService = intent -> {
            calls.add("PLANNING");
            return new PlanningServiceImpl().createPlan(intent);
        };
        ConfigurationService configurationService = plan -> {
            calls.add("CONFIGURATION");
            return new ConfigurationServiceImpl().generateConfig(plan);
        };
        ExecuteService executeService = (plan, configSet) -> {
            calls.add("EXECUTION");
            return new ExecuteServiceImpl().execute(plan, configSet);
        };
        VerificationService verificationService = (intent, plan, configSet, executionReport) -> {
            calls.add("VERIFICATION");
            return new VerificationServiceImpl().verify(intent, plan, configSet, executionReport);
        };
        TaskOrchestratorService orchestrator = new TaskOrchestratorServiceImpl(
                newWorkspaceService(),
                intentService,
                planningService,
                configurationService,
                executeService,
                verificationService
        );

        orchestrator.runDemoTask(RAW_TEXT);

        assertEquals(List.of("INTENT", "PLANNING", "CONFIGURATION", "EXECUTION", "VERIFICATION"), calls);
    }

    @Test
    void runDemoTaskMarksErrorWhenStageFails() {
        RecordingWorkspaceService workspaceService = new RecordingWorkspaceService();
        PlanningService failingPlanningService = intent -> {
            throw new IllegalStateException("planning boom");
        };
        TaskOrchestratorService orchestrator = new TaskOrchestratorServiceImpl(
                workspaceService,
                new IntentServiceImpl(),
                failingPlanningService,
                new ConfigurationServiceImpl(),
                new ExecuteServiceImpl(),
                new VerificationServiceImpl()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orchestrator.runDemoTask(RAW_TEXT)
        );
        NetworkWorkspace workspace = workspaceService.getWorkspace(workspaceService.getLastTaskId());

        assertEquals(ErrorCode.PIPELINE_FAILED, exception.getErrorCode());
        assertTrue(workspace.getTask().getTaskStatus() == TaskStatus.ERROR
                || workspace.getTask().getTaskStatus() == TaskStatus.FAILED);
        assertEquals(WorkflowStage.PLANNING.name(), workspace.getTask().getCurrentStage());
        assertTrue(workspace.getAgentLogs().stream().anyMatch(this::isPlanningFailureLog));
    }

    private TaskOrchestratorService orchestrator(NetworkWorkspaceService workspaceService) {
        return new TaskOrchestratorServiceImpl(
                workspaceService,
                new IntentServiceImpl(),
                new PlanningServiceImpl(),
                new ConfigurationServiceImpl(),
                new ExecuteServiceImpl(),
                new VerificationServiceImpl()
        );
    }

    private NetworkWorkspaceService newWorkspaceService() {
        return new NetworkWorkspaceServiceImpl(new InMemoryWorkspaceRepository());
    }

    private void assertCompletedStageLogs(NetworkWorkspace workspace) {
        Set<String> completedStages = workspace.getAgentLogs().stream()
                .filter(log -> log.getStageStatus() == StageStatus.SUCCESS)
                .map(AgentStepLog::getStage)
                .collect(Collectors.toSet());
        assertTrue(completedStages.containsAll(Set.of(
                WorkflowStage.INTENT.name(),
                WorkflowStage.PLANNING.name(),
                WorkflowStage.CONFIGURATION.name(),
                WorkflowStage.EXECUTION.name(),
                WorkflowStage.VERIFICATION.name()
        )));
    }

    private boolean isPlanningFailureLog(AgentStepLog log) {
        return WorkflowStage.PLANNING.name().equals(log.getStage())
                && log.getStageStatus() == StageStatus.FAILED
                && log.getMessage() != null
                && log.getMessage().contains("planning boom");
    }

    private static class RecordingWorkspaceService extends NetworkWorkspaceServiceImpl {

        private String lastTaskId;

        RecordingWorkspaceService() {
            super(new InMemoryWorkspaceRepository());
        }

        @Override
        public NetworkWorkspace createTask(String rawText) {
            NetworkWorkspace workspace = super.createTask(rawText);
            lastTaskId = workspace.getTask().getTaskId();
            return workspace;
        }

        String getLastTaskId() {
            return lastTaskId;
        }
    }
}
