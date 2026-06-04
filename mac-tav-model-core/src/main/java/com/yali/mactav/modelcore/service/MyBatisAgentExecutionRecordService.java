package com.yali.mactav.modelcore.service;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.workspace.AgentExecutionRecord;
import com.yali.mactav.modelcore.assembler.MyBatisModelCoreAssembler;
import com.yali.mactav.modelcore.repository.MyBatisAgentExecutionRecordRepository;
import com.yali.mactav.modelcore.repository.MyBatisNetworkTaskRepository;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;
import java.util.List;

/**
 * MyBatis-backed service for durable agent execution records.
 */
public class MyBatisAgentExecutionRecordService implements AgentExecutionRecordService {

    private final MyBatisAgentExecutionRecordRepository repository;
    private final MyBatisNetworkTaskRepository taskRepository;
    private final WorkspaceStateValidator workspaceStateValidator;
    private final MyBatisModelCoreAssembler assembler;

    public MyBatisAgentExecutionRecordService(MyBatisAgentExecutionRecordRepository repository,
                                              MyBatisNetworkTaskRepository taskRepository,
                                              WorkspaceStateValidator workspaceStateValidator,
                                              MyBatisModelCoreAssembler assembler) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.workspaceStateValidator = workspaceStateValidator;
        this.assembler = assembler;
    }

    @Override
    public AgentExecutionRecord appendRecord(String taskId, AgentExecutionRecord record) {
        workspaceStateValidator.validateTaskId(taskId);
        if (record == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATE_INVALID, "AgentExecutionRecord must not be null");
        }
        taskRepository.findByTaskId(taskId).orElseThrow(() -> workspaceStateValidator.workspaceNotFound(taskId));
        if (record.getTaskId() == null || record.getTaskId().isBlank()) {
            record.setTaskId(taskId);
        }
        repository.append(assembler.toEntity(record));
        return record;
    }

    @Override
    public List<AgentExecutionRecord> listRecords(String taskId) {
        workspaceStateValidator.validateTaskId(taskId);
        return repository.listByTaskId(taskId).stream().map(assembler::toRecord).toList();
    }
}
