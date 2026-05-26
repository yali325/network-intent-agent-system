package com.yali.mactav.model.intent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared payload contract used by Orchestrator to invoke IntentAgent over A2A.
 *
 * <p>This DTO belongs to mac-tav-model so Orchestrator and IntentAgent can
 * exchange intent-stage context without a Maven dependency on concrete agent
 * implementation classes.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentAgentInvokePayload {

    private String taskId;

    private String rawText;

    private Integer intentVersion;

    private String traceId;

    private String userContext;

    private String workspaceSnapshot;

    private String targetEnvironmentHint;

    private String createdBy;
}
