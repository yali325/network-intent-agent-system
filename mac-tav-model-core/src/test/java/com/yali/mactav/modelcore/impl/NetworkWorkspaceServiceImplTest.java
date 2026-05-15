package com.yali.mactav.modelcore.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.enums.TaskStatus;
import com.yali.mactav.common.enums.ValidationStatus;
import com.yali.mactav.common.enums.WorkflowStage;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.AgentStepLog;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceRepository;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetworkWorkspaceServiceImplTest {

    private NetworkWorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new NetworkWorkspaceServiceImpl(new InMemoryWorkspaceRepository());
    }

    @Test
    void createTaskCanCreateWorkspace() {
        NetworkWorkspace workspace = service.createTask("build an isolated office and guest network");

        assertNotNull(workspace.getTask());
        assertNotNull(workspace.getTask().getTaskId());
        assertEquals("build an isolated office and guest network", workspace.getTask().getRawText());
        assertEquals(TaskStatus.CREATED, workspace.getTask().getTaskStatus());
        assertEquals(WorkflowStage.CREATED.name(), workspace.getTask().getCurrentStage());
        assertEquals(0, workspace.getCurrentIntentVersion());
        assertEquals(0, workspace.getCurrentPlanVersion());
        assertEquals(0, workspace.getCurrentConfigVersion());
        assertEquals(0, workspace.getCurrentExecutionVersion());
        assertEquals(0, workspace.getCurrentValidationVersion());
        assertNotNull(workspace.getAgentLogs());
        assertTrue(workspace.getAgentLogs().isEmpty());
    }

    @Test
    void saveIntentCanBeQueried() {
        String taskId = service.createTask("raw").getTask().getTaskId();
        NetworkIntent intent = NetworkIntent.builder().intentVersion(2).build();

        service.saveIntent(taskId, intent);
        NetworkWorkspace workspace = service.getWorkspace(taskId);

        assertSame(intent, workspace.getIntent());
        assertEquals(taskId, workspace.getIntent().getTaskId());
        assertEquals(2, workspace.getCurrentIntentVersion());
        assertEquals(TaskStatus.PARSED, workspace.getTask().getTaskStatus());
        assertEquals(WorkflowStage.INTENT.name(), workspace.getTask().getCurrentStage());
        assertFalse(workspace.getAgentLogs().isEmpty());
    }

    @Test
    void savePlanCanBeQueried() {
        String taskId = service.createTask("raw").getTask().getTaskId();
        NetworkPlan plan = NetworkPlan.builder().planVersion(3).build();

        service.savePlan(taskId, plan);
        NetworkWorkspace workspace = service.getWorkspace(taskId);

        assertSame(plan, workspace.getPlan());
        assertEquals(taskId, workspace.getPlan().getTaskId());
        assertEquals(3, workspace.getCurrentPlanVersion());
        assertEquals(TaskStatus.PLANNED, workspace.getTask().getTaskStatus());
        assertEquals(WorkflowStage.PLANNING.name(), workspace.getTask().getCurrentStage());
    }

    @Test
    void saveConfigSetCanBeQueried() {
        String taskId = service.createTask("raw").getTask().getTaskId();
        ConfigSet configSet = ConfigSet.builder().configVersion(4).build();

        service.saveConfigSet(taskId, configSet);
        NetworkWorkspace workspace = service.getWorkspace(taskId);

        assertSame(configSet, workspace.getConfigSet());
        assertEquals(taskId, workspace.getConfigSet().getTaskId());
        assertEquals(4, workspace.getCurrentConfigVersion());
        assertEquals(TaskStatus.GENERATED, workspace.getTask().getTaskStatus());
        assertEquals(WorkflowStage.CONFIGURATION.name(), workspace.getTask().getCurrentStage());
    }

    @Test
    void saveExecutionReportCanBeQueried() {
        String taskId = service.createTask("raw").getTask().getTaskId();
        ExecutionReport executionReport = ExecutionReport.builder().executionVersion(5).build();

        service.saveExecutionReport(taskId, executionReport);
        NetworkWorkspace workspace = service.getWorkspace(taskId);

        assertSame(executionReport, workspace.getExecutionReport());
        assertEquals(taskId, workspace.getExecutionReport().getTaskId());
        assertEquals(5, workspace.getCurrentExecutionVersion());
        assertEquals(TaskStatus.EXECUTED, workspace.getTask().getTaskStatus());
        assertEquals(WorkflowStage.EXECUTION.name(), workspace.getTask().getCurrentStage());
    }

    @Test
    void saveValidationReportCanBeQueried() {
        String taskId = service.createTask("raw").getTask().getTaskId();
        ValidationReport validationReport = ValidationReport.builder()
                .validationVersion(6)
                .overallStatus(ValidationStatus.PASSED)
                .build();

        service.saveValidationReport(taskId, validationReport);
        NetworkWorkspace workspace = service.getWorkspace(taskId);

        assertSame(validationReport, workspace.getValidationReport());
        assertEquals(taskId, workspace.getValidationReport().getTaskId());
        assertEquals(6, workspace.getCurrentValidationVersion());
        assertEquals(TaskStatus.PASSED, workspace.getTask().getTaskStatus());
        assertEquals(WorkflowStage.VERIFICATION.name(), workspace.getTask().getCurrentStage());
    }

    @Test
    void appendAgentLogIncreasesLogCount() {
        String taskId = service.createTask("raw").getTask().getTaskId();
        AgentStepLog log = AgentStepLog.builder().message("manual log").build();

        NetworkWorkspace workspace = service.appendAgentLog(taskId, log);

        assertEquals(1, workspace.getAgentLogs().size());
        assertEquals(taskId, workspace.getAgentLogs().get(0).getTaskId());
        assertNotNull(workspace.getAgentLogs().get(0).getStepId());
    }

    @Test
    void getWorkspaceThrowsWhenTaskDoesNotExist() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getWorkspace("missing-task")
        );

        assertEquals(ErrorCode.TASK_NOT_FOUND, exception.getErrorCode());
    }
}
