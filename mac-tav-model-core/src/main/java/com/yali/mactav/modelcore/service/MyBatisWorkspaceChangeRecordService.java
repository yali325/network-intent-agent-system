package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.workspace.WorkspaceChangeRecord;
import com.yali.mactav.modelcore.assembler.MyBatisModelCoreAssembler;
import com.yali.mactav.modelcore.query.QueryPageSupport;
import com.yali.mactav.modelcore.query.WorkspaceChangeQuery;
import com.yali.mactav.modelcore.repository.MyBatisNetworkTaskRepository;
import com.yali.mactav.modelcore.repository.MyBatisWorkspaceChangeRecordRepository;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.util.List;

/**
 * MyBatis-backed service for durable workspace change records.
 */
public class MyBatisWorkspaceChangeRecordService implements WorkspaceChangeRecordService {

    private final MyBatisWorkspaceChangeRecordRepository repository;
    private final MyBatisNetworkTaskRepository taskRepository;
    private final WorkspaceStateValidator workspaceStateValidator;
    private final MyBatisModelCoreAssembler assembler;

    public MyBatisWorkspaceChangeRecordService(MyBatisWorkspaceChangeRecordRepository repository,
                                               MyBatisNetworkTaskRepository taskRepository,
                                               WorkspaceStateValidator workspaceStateValidator,
                                               MyBatisModelCoreAssembler assembler) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.workspaceStateValidator = workspaceStateValidator;
        this.assembler = assembler;
    }

    @Override
    public WorkspaceChangeRecord appendChange(String taskId, WorkspaceChangeRecord change) {
        workspaceStateValidator.validateTaskId(taskId);
        if (change == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "WorkspaceChangeRecord must not be null");
        }
        taskRepository.findByTaskId(taskId).orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        if (change.getTaskId() == null || change.getTaskId().isBlank()) {
            change.setTaskId(taskId);
        }
        repository.append(assembler.toEntity(change));
        return change;
    }

    @Override
    public List<WorkspaceChangeRecord> listChanges(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return repository.listByTaskId(taskId).stream().map(assembler::toChange).toList();
    }

    @Override
    public PageResult<WorkspaceChangeRecord> listChanges(String taskId, WorkspaceChangeQuery query) {
        workspaceStateValidator.validateTaskId(taskId);
        taskRepository.findByTaskId(taskId).orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        WorkspaceChangeQuery normalized = query == null ? new WorkspaceChangeQuery(null, null, null, null, 1, 20) : query;
        int page = QueryPageSupport.page(normalized.page());
        int size = QueryPageSupport.size(normalized.size());
        List<WorkspaceChangeRecord> items = repository.listByQuery(
                        taskId,
                        normalized.stage(),
                        normalized.changeType(),
                        normalized.from(),
                        normalized.to(),
                        size,
                        QueryPageSupport.offset(page, size))
                .stream()
                .map(assembler::toChange)
                .toList();
        long total = repository.countByQuery(
                taskId,
                normalized.stage(),
                normalized.changeType(),
                normalized.from(),
                normalized.to());
        return PageResult.<WorkspaceChangeRecord>builder()
                .items(items)
                .page(page)
                .size(size)
                .total(total)
                .build();
    }
}
