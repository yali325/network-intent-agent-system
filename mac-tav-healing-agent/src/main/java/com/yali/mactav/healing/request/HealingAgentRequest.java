package com.yali.mactav.healing.request;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal input request for the MAC-TAV HealingAgent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealingAgentRequest {

    private String taskId;

    private String rawText;

    private Integer validationVersion;

    private Integer repairVersion;

    private String validationReportJson;

    private String workspaceSnapshot;

    @Builder.Default
    private List<String> failedValidationItemIds = new ArrayList<>();

    private String failedValidationItemsJson;

    private String evidencesJson;

    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    private String traceRefsJson;

    private String traceId;

    private String userContext;

    private String createdBy;
}
