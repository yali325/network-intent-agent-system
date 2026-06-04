package com.yali.mactav.modelcore.mapper;

import com.yali.mactav.modelcore.entity.NetworkTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for network_task rows.
 */
@Mapper
public interface NetworkTaskMapper {

    int insert(NetworkTaskEntity entity);

    int update(NetworkTaskEntity entity);

    NetworkTaskEntity findByTaskId(@Param("taskId") String taskId);
}
