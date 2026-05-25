package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceEventRepository;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.util.List;

/**
 * In-memory event append/read service for workspace timelines.
 *
 * <p>It keeps event history local to the process for tests and early phases.
 * TODO Phase 9: persist events and optionally mirror them to Redis/SSE streams.</p>
 */
public class InMemoryWorkspaceEventService implements WorkspaceEventService {

    private final InMemoryWorkspaceEventRepository eventRepository;

    private final WorkspaceStateValidator workspaceStateValidator;

    public InMemoryWorkspaceEventService(
            InMemoryWorkspaceEventRepository eventRepository,
            WorkspaceStateValidator workspaceStateValidator) {
        this.eventRepository = eventRepository;
        this.workspaceStateValidator = workspaceStateValidator;
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
        return eventRepository.append(taskId, event);
    }

    @Override
    public List<WorkspaceEvent> listEvents(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return eventRepository.listByTaskId(taskId);
    }
}
