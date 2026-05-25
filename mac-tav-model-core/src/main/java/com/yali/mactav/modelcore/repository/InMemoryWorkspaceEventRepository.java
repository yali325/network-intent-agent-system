package com.yali.mactav.modelcore.repository;

import com.yali.mactav.model.workspace.WorkspaceEvent;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO Phase 9: replace this in-memory store with MySQL/Redis backed persistence.
 */
/**
 * Process-local repository for workspace events.
 *
 * <p>It supports early offline tests only. Phase 9 persistence/SSE integration
 * should provide durable ordering and delivery semantics.</p>
 */
public class InMemoryWorkspaceEventRepository {

    private final ConcurrentMap<String, CopyOnWriteArrayList<WorkspaceEvent>> eventsByTaskId =
            new ConcurrentHashMap<>();

    public WorkspaceEvent append(String taskId, WorkspaceEvent event) {
        eventsByTaskId.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()).add(event);
        return event;
    }

    public List<WorkspaceEvent> listByTaskId(String taskId) {
        return List.copyOf(eventsByTaskId.getOrDefault(taskId, new CopyOnWriteArrayList<>()));
    }
}
