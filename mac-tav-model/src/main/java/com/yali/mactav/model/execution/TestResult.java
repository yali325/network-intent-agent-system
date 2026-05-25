package com.yali.mactav.model.execution;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {

    @Builder.Default
    private List<ConnectivityTestResult> connectivityTests = new ArrayList<>();

    @Builder.Default
    private List<PolicyTestResult> policyTests = new ArrayList<>();

    @Builder.Default
    private List<PerformanceTestResult> performanceTests = new ArrayList<>();

    @Builder.Default
    private List<String> rawLogs = new ArrayList<>();
}
