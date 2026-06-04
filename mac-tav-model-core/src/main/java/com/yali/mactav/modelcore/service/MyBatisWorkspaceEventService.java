package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.assembler.MyBatisModelCoreAssembler;
import com.yali.mactav.modelcore.repository.MyBatisWorkspaceEventRepository;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.util.List;

/**
 * MyBatis-backed event service for durable workspace timeline events.
 */
public class MyBatisWorkspaceEventService implements WorkspaceEventService {

    private final MyBatisWorkspaceEventRepository repository;
    private final WorkspaceStateValidator workspaceStateValidator;
    private final MyBatisModelCoreAssembler assembler;

    public MyBatisWorkspaceEventService(MyBatisWorkspaceEventRepository repository,
                                        WorkspaceStateValidator workspaceStateValidator,
                                        MyBatisModelCoreAssembler assembler) {
        this.repository = repository;
        this.workspaceStateValidator = workspaceStateValidator;
        this.assembler = assembler;
    }

    @Override
    public WorkspaceEvent appendEvent(String taskId, WorkspaceEvent event) {
        workspaceStateValidator.validateTaskId(taskId);
        if (event == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "WorkspaceEvent must not be null");
        }
        if (event.getTaskId() == null || event.getTaskId().isBlank()) {
            event.setTaskId(taskId);
        }
        repository.append(assembler.toEntity(event));
        return event;
    }

    @Override
    public List<WorkspaceEvent> listEvents(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return repository.listByTaskId(taskId).stream().map(assembler::toEvent).toList();
    }
}
