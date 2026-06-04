package com.yali.mactav.modelcore.mapper;

import com.yali.mactav.modelcore.entity.AgentExecutionRecordEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for agent_execution_record rows.
 */
@Mapper
public interface AgentExecutionRecordMapper {

    int insert(AgentExecutionRecordEntity entity);

    List<AgentExecutionRecordEntity> listByTaskId(@Param("taskId") String taskId);
}
