package com.yali.mactav.modelcore.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * MySQL row for the network_task table.
 */
@Data
public class NetworkTaskEntity {

    private String taskId;
    private String rawText;
    private String description;
    private String taskStatus;
    private String currentStage;
    private String createdBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
