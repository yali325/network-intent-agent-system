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
public class PerformanceTestResult {

    private String testId;

    private String metric;

    private String expectedValue;

    private String actualValue;

    private String unit;

    private Boolean passed;

    private String message;

    private TraceRefs traceRefs;
}
