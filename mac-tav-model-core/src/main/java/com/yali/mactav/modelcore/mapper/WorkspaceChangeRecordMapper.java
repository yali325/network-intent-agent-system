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

    List<WorkspaceChangeRecordEntity> listByQuery(@Param("taskId") String taskId,
                                                  @Param("stage") String stage,
                                                  @Param("changeType") String changeType,
                                                  @Param("from") java.time.LocalDateTime from,
                                                  @Param("to") java.time.LocalDateTime to,
                                                  @Param("limit") int limit,
                                                  @Param("offset") int offset);

    long countByQuery(@Param("taskId") String taskId,
                      @Param("stage") String stage,
                      @Param("changeType") String changeType,
                      @Param("from") java.time.LocalDateTime from,
                      @Param("to") java.time.LocalDateTime to);
}
