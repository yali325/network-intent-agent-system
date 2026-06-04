package com.yali.mactav.modelcore.repository;

import com.yali.mactav.modelcore.entity.AgentExecutionRecordEntity;
import com.yali.mactav.modelcore.mapper.AgentExecutionRecordMapper;
import java.util.List;

/**
 * Repository wrapper for execution record persistence through MyBatis.
 */
public class MyBatisAgentExecutionRecordRepository {

    private final AgentExecutionRecordMapper mapper;

    public MyBatisAgentExecutionRecordRepository(AgentExecutionRecordMapper mapper) {
        this.mapper = mapper;
    }

    public AgentExecutionRecordEntity append(AgentExecutionRecordEntity entity) {
        mapper.insert(entity);
        return entity;
    }

    public List<AgentExecutionRecordEntity> listByTaskId(String taskId) {
        return mapper.listByTaskId(taskId);
    }
}
