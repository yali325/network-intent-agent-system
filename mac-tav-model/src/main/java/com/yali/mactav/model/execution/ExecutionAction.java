package com.yali.mactav.model.execution;

import com.yali.mactav.model.workspace.TraceRefs;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured action that an ExecutionAdapter may translate into controlled runtime operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionAction {

    private String actionId;

    private Integer sequence;

    private ExecutionActionType actionType;

    private String targetNodeId;

    private String targetDeviceId;

    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    private TraceRefs traceRefs;
}
