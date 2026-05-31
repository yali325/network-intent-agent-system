package com.yali.mactav.execution.safety;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.execution.ExecutionAction;
import com.yali.mactav.model.execution.ExecutionCommand;
import com.yali.mactav.model.execution.ExecutionPlan;
import com.yali.mactav.model.execution.TestCommand;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Validates that execution plans contain only allow-listed structured actions.
 */
public class ExecutionActionValidator {

    private final AllowedExecutionActionRegistry allowedActions;

    private final ExecutionCommandClassifier commandClassifier;

    public ExecutionActionValidator() {
        this(new AllowedExecutionActionRegistry(), new ExecutionCommandClassifier());
    }

    public ExecutionActionValidator(
            AllowedExecutionActionRegistry allowedActions,
            ExecutionCommandClassifier commandClassifier) {
        this.allowedActions = allowedActions;
        this.commandClassifier = commandClassifier;
    }

    public void validatePlan(ExecutionPlan plan) {
        if (plan == null) {
            throw forbidden("executionPlan", "ExecutionPlan must not be null");
        }
        requireTraceRefs("executionPlan", plan.getTraceRefs());
        validateActions("actions", plan.getActions());
        validateActions("cleanupActions", plan.getCleanupActions());
        validateTestCommands(plan.getTestCommands());
    }

    public void validateAction(ExecutionAction action) {
        if (action == null) {
            throw forbidden("action", "ExecutionAction must not be null");
        }
        if (isBlank(action.getActionId())) {
            throw forbidden("action", "ExecutionAction actionId must not be blank");
        }
        if (!allowedActions.isAllowed(action.getActionType())) {
            throw forbidden(action.getActionId(), "ExecutionAction actionType is not allow-listed: "
                    + action.getActionType());
        }
        requireTraceRefs(action.getActionId(), action.getTraceRefs());
        commandClassifier.rejectForbiddenParameters(action.getActionId(), action.getParameters());
    }

    public void validateLegacyCommand(ExecutionCommand command) {
        if (command == null) {
            throw forbidden("executionCommand", "ExecutionCommand must not be null");
        }
        if (isBlank(command.getCommandId())) {
            throw forbidden("executionCommand", "ExecutionCommand commandId must not be blank");
        }
        if (!allowedActions.isAllowed(command.getActionType())) {
            throw forbidden(command.getCommandId(), "ExecutionCommand actionType is not allow-listed: "
                    + command.getActionType());
        }
        requireTraceRefs(command.getCommandId(), command.getTraceRefs());
        commandClassifier.rejectForbiddenParameters(command.getCommandId(), command.getParameters());
    }

    public void validateTestCommand(TestCommand testCommand) {
        if (testCommand == null) {
            throw forbidden("testCommand", "TestCommand must not be null");
        }
        if (isBlank(testCommand.getTestId())) {
            throw forbidden("testCommand", "TestCommand testId must not be blank");
        }
        if (!allowedActions.isAllowed(testCommand.getTestType())) {
            throw forbidden(testCommand.getTestId(), "TestCommand testType is not allow-listed: "
                    + testCommand.getTestType());
        }
        requireTraceRefs(testCommand.getTestId(), testCommand.getTraceRefs());
        commandClassifier.rejectForbiddenParameters(testCommand.getTestId(), testCommand.getParameters());
    }

    private void validateActions(String fieldName, List<ExecutionAction> actions) {
        if (actions == null) {
            return;
        }
        for (ExecutionAction action : actions) {
            validateAction(action);
        }
    }

    private void validateTestCommands(List<TestCommand> testCommands) {
        if (testCommands == null) {
            return;
        }
        for (TestCommand testCommand : testCommands) {
            validateTestCommand(testCommand);
        }
    }

    private void requireTraceRefs(String itemId, TraceRefs traceRefs) {
        if (traceRefs == null || !hasAnyTrace(traceRefs)) {
            throw forbidden(itemId, "traceRefs must not be empty");
        }
    }

    private boolean hasAnyTrace(TraceRefs traceRefs) {
        return hasAny(traceRefs.getIntentNodeIds())
                || hasAny(traceRefs.getIntentRelationIds())
                || hasAny(traceRefs.getPlanElementIds())
                || hasAny(traceRefs.getConfigBlockIds())
                || hasAny(traceRefs.getTestIds())
                || hasAny(traceRefs.getValidationItemIds())
                || hasAny(traceRefs.getRepairActionIds());
    }

    private boolean hasAny(List<String> values) {
        return values != null && values.stream().anyMatch(value -> value != null && !value.isBlank());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BusinessException forbidden(String itemId, String message) {
        return new BusinessException(
                ErrorCode.EXECUTION_FORBIDDEN_COMMAND,
                "[" + itemId.toLowerCase(Locale.ROOT) + "] " + message);
    }
}
