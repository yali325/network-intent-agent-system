package com.yali.mactav.model.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A payload for invoking the VerificationAgent with current stage artifacts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationAgentInvokePayload {

    private String taskId;

    private String rawText;

    private Integer intentVersion;

    private Integer planVersion;

    private Integer configVersion;

    private Integer executionVersion;

    private Integer validationVersion;

    private String intentJson;

    private String planJson;

    private String configSetJson;

    private String executionReportJson;

    private String traceId;

    private String userContext;

    private String workspaceSnapshot;

    private String createdBy;
}
