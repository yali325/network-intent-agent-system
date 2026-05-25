package com.yali.mactav.modelcore.validator;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.task.NetworkTask;
import com.yali.mactav.model.workspace.NetworkWorkspace;

/**
 * 负责校验 Workspace / Task 状态是否合法。
 */
/**
 * Validates workspace/task identity and converts missing state into common errors.
 *
 * <p>This class guards model-core state operations. It does not implement a
 * full workflow state machine.</p>
 */
public class WorkspaceStateValidator {

    public void validateTask(NetworkTask task) {
        if (task == null) {
            throw invalidState("NetworkTask must not be null");
        }
        validateTaskId(task.getTaskId());
    }

    public void validateWorkspace(NetworkWorkspace workspace) {
        if (workspace == null || workspace.getTask() == null) {
            throw invalidState("NetworkWorkspace and task must not be null");
        }
        validateTaskId(workspace.getTask().getTaskId());
    }

    public void validateTaskId(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw invalidState("taskId must not be blank");
        }
    }

    public BusinessException workspaceNotFound(String taskId) {
        return new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, "Workspace not found: " + taskId);
    }

    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, message);
    }
}
