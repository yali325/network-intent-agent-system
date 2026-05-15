package com.yali.mactav.model.task;

import com.yali.mactav.common.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkTask {

    private String taskId;
    private String rawText;
    private TaskStatus taskStatus;
    private String currentStage;
    private String createdAt;
    private String updatedAt;
}
