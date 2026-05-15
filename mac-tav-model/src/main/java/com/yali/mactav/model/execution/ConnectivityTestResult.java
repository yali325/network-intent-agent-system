package com.yali.mactav.model.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectivityTestResult {

    private String testId;
    private String source;
    private String target;
    private String expected;
    private String actual;
    private Boolean success;
    private String rawOutput;
}
