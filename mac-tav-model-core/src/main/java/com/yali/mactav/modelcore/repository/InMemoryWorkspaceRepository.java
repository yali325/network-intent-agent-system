package com.yali.mactav.modelcore.repository;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryWorkspaceRepository {

    private final Map<String, NetworkWorkspace> workspaceStore = new ConcurrentHashMap<>();

    public NetworkWorkspace save(NetworkWorkspace workspace) {
        if (workspace == null || workspace.getTask() == null || isBlank(workspace.getTask().getTaskId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Workspace taskId must not be blank");
        }
        workspaceStore.put(workspace.getTask().getTaskId(), workspace);
        return workspace;
    }

    public NetworkWorkspace findByTaskId(String taskId) {
        if (isBlank(taskId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "TaskId must not be blank");
        }
        NetworkWorkspace workspace = workspaceStore.get(taskId);
        if (workspace == null) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND, "Task not found: " + taskId);
        }
        return workspace;
    }

    public boolean existsByTaskId(String taskId) {
        return !isBlank(taskId) && workspaceStore.containsKey(taskId);
    }

    public void clear() {
        workspaceStore.clear();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
