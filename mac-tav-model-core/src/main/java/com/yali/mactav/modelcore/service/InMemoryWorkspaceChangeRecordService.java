package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.modelcore.query.QueryPageSupport;
import com.yali.mactav.modelcore.query.WorkspaceChangeQuery;
import com.yali.mactav.modelcore.repository.InMemoryNetworkWorkspaceRepository;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceChangeRecordRepository;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory storage service for workspace change audit records.
 *
 * <p>It records changes selected elsewhere, usually by Orchestrator. TODO Phase
 * 9: move audit records to durable storage.</p>
 */
public class InMemoryWorkspaceChangeRecordService implements WorkspaceChangeRecordService {

    private final InMemoryWorkspaceChangeRecordRepository changeRepository;

    private final InMemoryNetworkWorkspaceRepository workspaceRepository;

    private final WorkspaceStateValidator workspaceStateValidator;

    public InMemoryWorkspaceChangeRecordService(
            InMemoryWorkspaceChangeRecordRepository changeRepository,
            InMemoryNetworkWorkspaceRepository workspaceRepository,
            WorkspaceStateValidator workspaceStateValidator) {
        this.changeRepository = changeRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceStateValidator = workspaceStateValidator;
    }

    @Override
    public WorkspaceChangeRecord appendChange(String taskId, WorkspaceChangeRecord change) {
        workspaceStateValidator.validateTaskId(taskId);
        NetworkWorkspace workspace = workspaceRepository
                .findByTaskId(taskId)
                .orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        if (change.getTaskId() == null || change.getTaskId().isBlank()) {
            change.setTaskId(taskId);
        }
        if (workspace.getChangeHistory() == null) {
            workspace.setChangeHistory(new ArrayList<>());
        }
        workspace.getChangeHistory().add(change);
        workspaceRepository.save(taskId, workspace);
        return changeRepository.append(taskId, change);
    }

    @Override
    public List<WorkspaceChangeRecord> listChanges(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return changeRepository.listByTaskId(taskId);
    }

    @Override
    public PageResult<WorkspaceChangeRecord> listChanges(String taskId, WorkspaceChangeQuery query) {
        workspaceStateValidator.validateTaskId(taskId);
        WorkspaceChangeQuery normalized = query == null ? new WorkspaceChangeQuery(null, null, null, null, 1, 20) : query;
        int page = QueryPageSupport.page(normalized.page());
        int size = QueryPageSupport.size(normalized.size());
        List<WorkspaceChangeRecord> filtered = changeRepository.listByTaskId(taskId).stream()
                .filter(change -> normalized.stage() == null || normalized.stage() == change.getStage())
                .filter(change -> normalized.changeType() == null
                        || normalized.changeType().equals(change.getChangeType()))
                .filter(change -> normalized.from() == null
                        || (change.getCreateTime() != null && !change.getCreateTime().isBefore(normalized.from())))
                .filter(change -> normalized.to() == null
                        || (change.getCreateTime() != null && !change.getCreateTime().isAfter(normalized.to())))
                .toList();
        return PageResult.<WorkspaceChangeRecord>builder()
                .items(QueryPageSupport.slice(filtered, page, size))
                .page(page)
                .size(size)
                .total(filtered.size())
                .build();
    }
}
