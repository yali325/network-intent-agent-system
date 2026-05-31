package com.yali.mactav.model.execution;

import com.yali.mactav.model.workspace.TraceRefs;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Legacy controlled command descriptor retained as a structured action shape.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionCommand {

    private String commandId;

    private String targetNodeId;

    private ExecutionActionType actionType;

    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    private Boolean safeToRun;

    private TraceRefs traceRefs;
}
