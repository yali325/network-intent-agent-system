package com.yali.mactav.model.execution;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestResult {

    private List<ConnectivityTestResult> connectivityTests;
    private List<PolicyTestResult> policyTests;
    private List<String> rawLogs;
}
