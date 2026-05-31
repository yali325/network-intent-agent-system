package com.yali.mactav.model.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Shared payload contract used by Orchestrator to invoke ConfigurationAgent over A2A.
 *
 * <p>This DTO belongs to mac-tav-model so Orchestrator and ConfigurationAgent
 * can exchange configuration-stage context without a Maven dependency on
 * concrete agent implementation classes.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationAgentInvokePayload {

    private String taskId;

    private String rawText;

    private Integer intentVersion;

    private Integer planVersion;

    private String planJson;

    private Integer configVersion;

    private String traceId;

    private String userContext;

    private String workspaceSnapshot;

    private String targetEnvironmentHint;

    private String createdBy;
}
