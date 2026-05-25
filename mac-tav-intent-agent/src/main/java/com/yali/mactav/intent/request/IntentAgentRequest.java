package com.yali.mactav.intent.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal input request for the MAC-TAV IntentAgent offline chain.
 *
 * <p>This class belongs to mac-tav-intent-agent. It carries user intent text
 * and task context for parsing, but it is not a Web API request and must not
 * depend on Web, Orchestrator, Model Core, or concrete agent implementations.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentAgentRequest {

    private String taskId;

    private String rawText;

    private Integer intentVersion;

    private String traceId;

    private String userContext;

    private String workspaceSnapshot;

    private String targetEnvironmentHint;

    private String createdBy;
}
