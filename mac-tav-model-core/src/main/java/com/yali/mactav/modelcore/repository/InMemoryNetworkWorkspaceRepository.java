package com.yali.mactav.modelcore.repository;

import com.yali.mactav.model.workspace.NetworkWorkspace;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TODO Phase 9: replace this in-memory store with MySQL/Redis backed persistence.
 * 保存和查询 Workspace
 */
/**
 * Process-local repository for NetworkWorkspace objects.
 *
 * <p>This class is an implementation detail of mac-tav-model-core. It must not
 * be used as a cross-service persistence contract.</p>
 */
public class InMemoryNetworkWorkspaceRepository {

    private final ConcurrentMap<String, NetworkWorkspace> workspaces = new ConcurrentHashMap<>();

    public NetworkWorkspace save(String taskId, NetworkWorkspace workspace) {
        workspaces.put(taskId, workspace);
        return workspace;
    }

    public Optional<NetworkWorkspace> findByTaskId(String taskId) {
        return Optional.ofNullable(workspaces.get(taskId));
    }

    public boolean existsByTaskId(String taskId) {
        return workspaces.containsKey(taskId);
    }
}
