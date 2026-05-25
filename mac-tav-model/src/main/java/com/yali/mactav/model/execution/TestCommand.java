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
public class TestCommand {

    private String testId;

    private String type;

    private String sourceNode;

    private String targetNode;

    private String command;

    private String expectedResult;

    private TraceRefs traceRefs;
}
