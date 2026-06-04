package com.yali.mactav.modelcore.mapper;

import com.yali.mactav.modelcore.entity.WorkspaceChangeRecordEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for workspace_change_record rows.
 */
@Mapper
public interface WorkspaceChangeRecordMapper {

    int insert(WorkspaceChangeRecordEntity entity);

    List<WorkspaceChangeRecordEntity> listByTaskId(@Param("taskId") String taskId);
}
