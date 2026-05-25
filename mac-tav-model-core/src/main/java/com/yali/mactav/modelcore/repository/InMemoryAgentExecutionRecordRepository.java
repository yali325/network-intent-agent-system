package com.yali.mactav.modelcore.repository;

import com.yali.mactav.model.workspace.AgentExecutionRecord;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO Phase 9: replace this in-memory store with MySQL/Redis backed persistence.
 */
/**
 * Process-local repository for agent execution records.
 *
 * <p>It stores trace summaries for early phases and should be replaced by
 * durable audit storage in Phase 9.</p>
 */
public class InMemoryAgentExecutionRecordRepository {

    private final ConcurrentMap<String, CopyOnWriteArrayList<AgentExecutionRecord>> recordsByTaskId =
            new ConcurrentHashMap<>();

    public AgentExecutionRecord append(String taskId, AgentExecutionRecord record) {
        recordsByTaskId.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()).add(record);
        return record;
    }

    public List<AgentExecutionRecord> listByTaskId(String taskId) {
        return List.copyOf(recordsByTaskId.getOrDefault(taskId, new CopyOnWriteArrayList<>()));
    }
}
