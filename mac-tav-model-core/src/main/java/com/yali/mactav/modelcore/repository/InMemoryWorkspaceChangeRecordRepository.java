package com.yali.mactav.modelcore.repository;

import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO Phase 9: replace this in-memory store with MySQL/Redis backed persistence.
 */
/**
 * Process-local repository for workspace change history.
 *
 * <p>It records audit entries in memory only and should be replaced by durable
 * storage when persistence is introduced.</p>
 */
public class InMemoryWorkspaceChangeRecordRepository {

    private final ConcurrentMap<String, CopyOnWriteArrayList<WorkspaceChangeRecord>> changesByTaskId =
            new ConcurrentHashMap<>();

    public WorkspaceChangeRecord append(String taskId, WorkspaceChangeRecord change) {
        changesByTaskId.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()).add(change);
        return change;
    }

    public List<WorkspaceChangeRecord> listByTaskId(String taskId) {
        return List.copyOf(changesByTaskId.getOrDefault(taskId, new CopyOnWriteArrayList<>()));
    }
}
