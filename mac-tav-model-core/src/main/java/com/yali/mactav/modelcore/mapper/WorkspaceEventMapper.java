package com.yali.mactav.modelcore.mapper;

import com.yali.mactav.modelcore.entity.WorkspaceEventEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for workspace_event rows.
 */
@Mapper
public interface WorkspaceEventMapper {

    int insert(WorkspaceEventEntity entity);

    List<WorkspaceEventEntity> listByTaskId(@Param("taskId") String taskId);
}
