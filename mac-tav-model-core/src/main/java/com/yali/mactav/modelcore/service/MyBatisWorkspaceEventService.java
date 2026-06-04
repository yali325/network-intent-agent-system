package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.common.result.PageResult;
import com.yali.mactav.model.workspace.WorkspaceEvent;
import com.yali.mactav.modelcore.assembler.MyBatisModelCoreAssembler;
import com.yali.mactav.modelcore.event.WorkspaceEventPublisher;
import com.yali.mactav.modelcore.query.QueryPageSupport;
import com.yali.mactav.modelcore.query.WorkspaceEventQuery;
import com.yali.mactav.modelcore.repository.MyBatisWorkspaceEventRepository;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * MyBatis-backed event service for durable workspace timeline events.
 */
public class MyBatisWorkspaceEventService implements WorkspaceEventService {

    private static final Logger log = LoggerFactory.getLogger(MyBatisWorkspaceEventService.class);

    private final MyBatisWorkspaceEventRepository repository;
    private final WorkspaceStateValidator workspaceStateValidator;
    private final MyBatisModelCoreAssembler assembler;
    private final WorkspaceEventPublisher eventPublisher;

    public MyBatisWorkspaceEventService(MyBatisWorkspaceEventRepository repository,
                                        WorkspaceStateValidator workspaceStateValidator,
                                        MyBatisModelCoreAssembler assembler,
                                        WorkspaceEventPublisher eventPublisher) {
        this.repository = repository;
        this.workspaceStateValidator = workspaceStateValidator;
        this.assembler = assembler;
        this.eventPublisher = eventPublisher;
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
        publishAfterCommit(event);
        return event;
    }

    @Override
    public List<WorkspaceEvent> listEvents(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return repository.listByTaskId(taskId).stream().map(assembler::toEvent).toList();
    }

    @Override
    public PageResult<WorkspaceEvent> listEvents(String taskId, WorkspaceEventQuery query) {
        workspaceStateValidator.validateTaskId(taskId);
        WorkspaceEventQuery normalized = query == null ? new WorkspaceEventQuery(null, null, null, null, 1, 20) : query;
        int page = QueryPageSupport.page(normalized.page());
        int size = QueryPageSupport.size(normalized.size());
        List<WorkspaceEvent> items = repository.listByQuery(
                        taskId,
                        normalized.stage(),
                        normalized.eventType(),
                        normalized.from(),
                        normalized.to(),
                        size,
                        QueryPageSupport.offset(page, size))
                .stream()
                .map(assembler::toEvent)
                .toList();
        long total = repository.countByQuery(
                taskId,
                normalized.stage(),
                normalized.eventType(),
                normalized.from(),
                normalized.to());
        return PageResult.<WorkspaceEvent>builder()
                .items(items)
                .page(page)
                .size(size)
                .total(total)
                .build();
    }

    private void publishAfterCommit(WorkspaceEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishSafely(event);
                }
            });
            return;
        }
        publishSafely(event);
    }

    private void publishSafely(WorkspaceEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException exception) {
            log.warn(
                    "Workspace event persisted but Redis publish failed: taskId={}, eventId={}, eventType={}",
                    event.getTaskId(),
                    event.getEventId(),
                    event.getEventType(),
                    exception);
        }
    }
}
