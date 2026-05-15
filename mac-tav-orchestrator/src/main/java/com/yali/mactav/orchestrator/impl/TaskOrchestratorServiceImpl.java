package com.yali.mactav.orchestrator.impl;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.common.enums.TaskStatus;
import com.yali.mactav.common.enums.WorkflowStage;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.util.IdGenerator;
import com.yali.mactav.configuration.service.ConfigurationService;
import com.yali.mactav.execution.service.ExecuteService;
import com.yali.mactav.intent.service.IntentService;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.execution.ExecutionReport;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.model.workspace.AgentStepLog;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.orchestrator.service.TaskOrchestratorService;
import com.yali.mactav.planning.service.PlanningService;
import com.yali.mactav.verification.service.VerificationService;
import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class TaskOrchestratorServiceImpl implements TaskOrchestratorService {

    private static final String ORCHESTRATOR_AGENT = "TaskOrchestrator";

    private final NetworkWorkspaceService networkWorkspaceService;
    private final IntentService intentService;
    private final PlanningService planningService;
    private final ConfigurationService configurationService;
    private final ExecuteService executeService;
    private final VerificationService verificationService;

    public TaskOrchestratorServiceImpl(NetworkWorkspaceService networkWorkspaceService,
                                       IntentService intentService,
                                       PlanningService planningService,
                                       ConfigurationService configurationService,
                                       ExecuteService executeService,
                                       VerificationService verificationService) {
        this.networkWorkspaceService = networkWorkspaceService;
        this.intentService = intentService;
        this.planningService = planningService;
        this.configurationService = configurationService;
        this.executeService = executeService;
        this.verificationService = verificationService;
    }

    @Override
    public NetworkWorkspace runDemoTask(String rawText) {
        NetworkWorkspace workspace = networkWorkspaceService.createTask(rawText);
        String taskId = workspace.getTask().getTaskId();
        networkWorkspaceService.updateTaskStatus(taskId, TaskStatus.RUNNING);

        NetworkIntent intent = runStage(
                taskId,
                WorkflowStage.INTENT,
                "IntentAgent 开始意图解析",
                "IntentAgent 已完成意图解析",
                () -> {
                    NetworkIntent result = intentService.parseIntent(taskId, rawText);
                    networkWorkspaceService.saveIntent(taskId, result);
                    return result;
                }
        );

        NetworkPlan plan = runStage(
                taskId,
                WorkflowStage.PLANNING,
                "PlanningAgent 开始生成网络规划",
                "PlanningAgent 已生成网络规划",
                () -> {
                    NetworkPlan result = planningService.createPlan(intent);
                    networkWorkspaceService.savePlan(taskId, result);
                    return result;
                }
        );

        ConfigSet configSet = runStage(
                taskId,
                WorkflowStage.CONFIGURATION,
                "ConfigurationAgent 开始生成设备配置",
                "ConfigurationAgent 已生成设备配置",
                () -> {
                    ConfigSet result = configurationService.generateConfig(plan);
                    networkWorkspaceService.saveConfigSet(taskId, result);
                    return result;
                }
        );

        ExecutionReport executionReport = runStage(
                taskId,
                WorkflowStage.EXECUTION,
                "ExecuteModule 开始 DryRun 执行",
                "ExecuteModule 已完成 DryRun 执行",
                () -> {
                    ExecutionReport result = executeService.execute(plan, configSet);
                    networkWorkspaceService.saveExecutionReport(taskId, result);
                    return result;
                }
        );

        runStage(
                taskId,
                WorkflowStage.VERIFICATION,
                "VerificationAgent 开始验证评估",
                "VerificationAgent 已完成验证评估",
                () -> {
                    ValidationReport result = verificationService.verify(intent, plan, configSet, executionReport);
                    networkWorkspaceService.saveValidationReport(taskId, result);
                    return result;
                }
        );

        return networkWorkspaceService.getWorkspace(taskId);
    }

    private <T> T runStage(String taskId, WorkflowStage stage, String startMessage, String successMessage,
                           Supplier<T> supplier) {
        try {
            networkWorkspaceService.updateCurrentStage(taskId, stage);
            appendLog(taskId, stage, StageStatus.RUNNING, startMessage);
            T result = supplier.get();
            appendLog(taskId, stage, StageStatus.SUCCESS, successMessage);
            return result;
        } catch (Exception ex) {
            handleStageFailure(taskId, stage, ex);
            throw pipelineException(stage, ex);
        }
    }

    private void handleStageFailure(String taskId, WorkflowStage stage, Exception ex) {
        networkWorkspaceService.updateCurrentStage(taskId, stage);
        networkWorkspaceService.updateTaskStatus(taskId, TaskStatus.ERROR);
        appendLog(taskId, stage, StageStatus.FAILED, stage.name() + " 阶段失败：" + errorMessage(ex));
    }

    private BusinessException pipelineException(WorkflowStage stage, Exception ex) {
        if (ex instanceof BusinessException businessException
                && businessException.getErrorCode() == ErrorCode.PIPELINE_FAILED) {
            return businessException;
        }
        return new BusinessException(
                ErrorCode.PIPELINE_FAILED,
                "Pipeline failed at " + stage.name() + ": " + errorMessage(ex)
        );
    }

    private void appendLog(String taskId, WorkflowStage stage, StageStatus status, String message) {
        String now = Instant.now().toString();
        networkWorkspaceService.appendAgentLog(
                taskId,
                AgentStepLog.builder()
                        .stepId("step-" + IdGenerator.uuid())
                        .taskId(taskId)
                        .agentName(ORCHESTRATOR_AGENT)
                        .stage(stage.name())
                        .stageStatus(status)
                        .message(message)
                        .startedAt(now)
                        .finishedAt(status == StageStatus.RUNNING ? null : now)
                        .build()
        );
    }

    private String errorMessage(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getMessage();
    }
}
