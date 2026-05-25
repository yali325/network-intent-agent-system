package com.yali.mactav.modelcore.service;

import com.yali.mactav.model.workspace.AgentExecutionRecord;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.modelcore.repository.InMemoryAgentExecutionRecordRepository;
import com.yali.mactav.modelcore.repository.InMemoryNetworkWorkspaceRepository;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory storage service for AgentExecutionRecord summaries.
 *
 * <p>It records execution metadata into the workspace view but never invokes
 * remote agents or models. TODO Phase 9: move records to durable storage.</p>
 */
public class InMemoryAgentExecutionRecordService implements AgentExecutionRecordService {

    private final InMemoryAgentExecutionRecordRepository recordRepository;

    private final InMemoryNetworkWorkspaceRepository workspaceRepository;

    private final WorkspaceStateValidator workspaceStateValidator;

    public InMemoryAgentExecutionRecordService(
            InMemoryAgentExecutionRecordRepository recordRepository,
            InMemoryNetworkWorkspaceRepository workspaceRepository,
            WorkspaceStateValidator workspaceStateValidator) {
        this.recordRepository = recordRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceStateValidator = workspaceStateValidator;
    }

    @Override
    public AgentExecutionRecord appendRecord(String taskId, AgentExecutionRecord record) {
        workspaceStateValidator.validateTaskId(taskId);
        NetworkWorkspace workspace = workspaceRepository
                .findByTaskId(taskId)
                .orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        if (record.getTaskId() == null || record.getTaskId().isBlank()) {
            record.setTaskId(taskId);
        }
        if (workspace.getAgentExecutionRecords() == null) {
            workspace.setAgentExecutionRecords(new ArrayList<>());
        }
        workspace.getAgentExecutionRecords().add(record);
        workspaceRepository.save(taskId, workspace);
        return recordRepository.append(taskId, record);
    }

    @Override
    public List<AgentExecutionRecord> listRecords(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return recordRepository.listByTaskId(taskId);
    }
}
