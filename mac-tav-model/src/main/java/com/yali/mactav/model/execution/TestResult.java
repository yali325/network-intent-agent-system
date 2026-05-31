package com.yali.mactav.model.execution;

import com.yali.mactav.model.workspace.TraceRefs;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One structured execution test result, such as ping, traceroute, iperf, flow table, or controller state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {

    private String testId;

    private TestResultType testType;

    private TestResultStatus status;

    private String sourceNode;

    private String targetNode;

    private String expectedResult;

    private String actualResult;

    @Builder.Default
    private Map<String, Object> metrics = new HashMap<>();

    private String logsSummary;

    @Builder.Default
    private Map<String, String> evidenceRefs = new HashMap<>();

    private TraceRefs traceRefs;
}
