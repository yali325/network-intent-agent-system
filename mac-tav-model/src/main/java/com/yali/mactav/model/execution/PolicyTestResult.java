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
public class PolicyTestResult {

    private String testId;

    private String policyId;

    private String expected;

    private String actual;

    private Boolean passed;

    private String severity;

    private String message;

    private TraceRefs traceRefs;
}
