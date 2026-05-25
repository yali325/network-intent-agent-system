package com.yali.mactav.agent.core.context;

import com.yali.mactav.model.enums.WorkflowStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common execution context passed through parser and validator boundaries.
 *
 * <p>AgentRunContext is shared agent infrastructure. It may carry task/stage
 * metadata and a workspace snapshot string, but agents still must not mutate
 * NetworkWorkspace directly.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunContext {

    private String taskId;

    private WorkflowStage stage;

    private Integer version;

    private String traceId;

    private String userInput;

    private String workspaceSnapshot;
}
