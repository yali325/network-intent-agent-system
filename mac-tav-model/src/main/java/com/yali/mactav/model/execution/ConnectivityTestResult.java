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
public class ConnectivityTestResult {

    private String testId;

    private String sourceNode;

    private String targetNode;

    private String expectedResult;

    private String actualResult;

    private Boolean passed;

    private String message;

    private TraceRefs traceRefs;
}
