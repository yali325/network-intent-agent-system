package com.yali.mactav.modelcore.mapper;

import com.yali.mactav.modelcore.entity.NetworkWorkspaceStateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for network_workspace_state rows.
 */
@Mapper
public interface NetworkWorkspaceStateMapper {

    int insert(NetworkWorkspaceStateEntity entity);

    int update(NetworkWorkspaceStateEntity entity);

    NetworkWorkspaceStateEntity findByTaskId(@Param("taskId") String taskId);
}
