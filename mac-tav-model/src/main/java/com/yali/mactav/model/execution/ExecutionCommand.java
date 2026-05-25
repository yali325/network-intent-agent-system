package com.yali.mactav.model.execution;

import com.yali.mactav.model.workspace.TraceRefs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionCommand {

    private String commandId;

    private String targetNodeId;

    private String commandType;

    private String command;

    private Boolean safeToRun;

    private TraceRefs traceRefs;
}
