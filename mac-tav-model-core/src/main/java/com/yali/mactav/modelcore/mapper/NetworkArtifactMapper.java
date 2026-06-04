package com.yali.mactav.modelcore.mapper;

import com.yali.mactav.modelcore.entity.NetworkArtifactEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis mapper for network_artifact rows.
 */
@Mapper
public interface NetworkArtifactMapper {

    int insert(NetworkArtifactEntity entity);

    int update(NetworkArtifactEntity entity);

    int markSupersededExcept(@Param("taskId") String taskId,
                             @Param("artifactType") String artifactType,
                             @Param("artifactId") String artifactId);

    NetworkArtifactEntity findByArtifactId(@Param("artifactId") String artifactId);

    NetworkArtifactEntity findByTaskIdTypeVersion(@Param("taskId") String taskId,
                                                  @Param("artifactType") String artifactType,
                                                  @Param("version") Integer version);

    List<NetworkArtifactEntity> listByTaskId(@Param("taskId") String taskId);

    List<NetworkArtifactEntity> listByTaskIdAndType(@Param("taskId") String taskId,
                                                    @Param("artifactType") String artifactType);

    Integer findMaxVersion(@Param("taskId") String taskId, @Param("artifactType") String artifactType);
}
