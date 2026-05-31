package com.yali.mactav.model.execution;

import com.yali.mactav.model.workspace.TraceRefs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured failure reported by the execution stage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionError {

    private String errorId;

    private String errorCode;

    private String message;

    private String stage;

    private String actionId;

    private String relatedCommandId;

    private Boolean recoverable;

    private TraceRefs traceRefs;
}
