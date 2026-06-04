package com.yali.mactav.modelcore.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * MySQL row for agent and module execution audit records.
 */
@Data
public class AgentExecutionRecordEntity {

    private String recordId;
    private String taskId;
    private String traceId;
    private String agentName;
    private String targetAgentName;
    private String remoteCallType;
    private String agentCardVersion;
    private String stage;
    private String stageStatus;
    private String inputArtifactIdsJson;
    private String outputArtifactIdsJson;
    private String toolCallSummariesJson;
    private String mcpCallSummariesJson;
    private String a2aCallSummariesJson;
    private Integer modelCallCount;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private Long durationMs;
    private String inputSummary;
    private String outputSummary;
    private String errorCode;
    private String errorMessage;
    private String message;
}
