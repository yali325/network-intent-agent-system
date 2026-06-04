package com.yali.mactav.modelcore.mapper;

import com.yali.mactav.modelcore.entity.WorkflowJobEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for workflow_job CRUD.
 */
@Mapper
public interface WorkflowJobMapper {

    int insert(WorkflowJobEntity entity);

    int update(WorkflowJobEntity entity);

    WorkflowJobEntity findByJobId(@Param("jobId") String jobId);

    List<WorkflowJobEntity> listByTaskId(@Param("taskId") String taskId);
}
