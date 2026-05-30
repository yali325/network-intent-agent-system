package com.yali.mactav.planning.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal input request for the MAC-TAV PlanningAgent.
 *
 * <p>This class belongs to mac-tav-planning-agent. It carries the parsed
 * NetworkIntent and task context for conversion into NetworkPlan, but it is
 * not a Web API request and must not depend on Web, Orchestrator, Model Core,
 * or concrete agent implementations.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningAgentRequest {

    private String taskId;

    private String rawText;

    private Integer intentVersion;

    private String intentJson;

    private Integer planVersion;

    private String traceId;

    private String userContext;

    private String workspaceSnapshot;

    private String targetEnvironmentHint;

    private String createdBy;
}
