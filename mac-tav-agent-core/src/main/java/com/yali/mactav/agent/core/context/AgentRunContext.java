package com.yali.mactav.agent.core.context;

import com.yali.mactav.common.enums.WorkflowStage;
import com.yali.mactav.model.workspace.NetworkWorkspace;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunContext {

    public static final String CONTEXT_KEY = "mactavAgentRunContext";

    private String taskId;
    private WorkflowStage stage;
    private Integer version;
    private String traceId;
    private String userInput;
    private NetworkWorkspace workspaceSnapshot;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
