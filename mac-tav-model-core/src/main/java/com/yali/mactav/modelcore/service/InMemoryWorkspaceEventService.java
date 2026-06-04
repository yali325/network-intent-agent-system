package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.query.QueryPageSupport;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
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

    @Override
    public PageResult<WorkspaceEvent> listEvents(String taskId, WorkspaceEventQuery query) {
        workspaceStateValidator.validateTaskId(taskId);
        WorkspaceEventQuery normalized = query == null ? new WorkspaceEventQuery(null, null, null, null, 1, 20) : query;
        int page = QueryPageSupport.page(normalized.page());
        int size = QueryPageSupport.size(normalized.size());
        List<WorkspaceEvent> filtered = eventRepository.listByTaskId(taskId).stream()
                .filter(event -> normalized.stage() == null || normalized.stage() == event.getStage())
                .filter(event -> normalized.eventType() == null || normalized.eventType().equals(event.getEventType()))
                .filter(event -> normalized.from() == null
                        || (event.getEventTime() != null && !event.getEventTime().isBefore(normalized.from())))
                .filter(event -> normalized.to() == null
                        || (event.getEventTime() != null && !event.getEventTime().isAfter(normalized.to())))
                .toList();
        return PageResult.<WorkspaceEvent>builder()
                .items(QueryPageSupport.slice(filtered, page, size))
                .page(page)
                .size(size)
                .total(filtered.size())
                .build();
    }
}
