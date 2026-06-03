package com.yali.mactav.model.healing;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A2A payload for invoking HealingAgent with validation failure context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealingAgentInvokePayload {

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
