package com.yali.mactav.model.workspace;

import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.WorkflowStage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记录每个 Agent 或关键模块的执行过程
 */
/**
 * Structured execution summary for an agent, tool, MCP, A2A, or execution step.
 *
 * <p>This record is for traceability and timeline/audit views. It should not
 * store full secrets, raw credentials, or unredacted provider request headers.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionRecord {

    private String recordId;

    private String taskId;

    private String traceId;

    private String agentName;

    private String targetAgentName;

    private String remoteCallType;

    private String agentCardVersion;

    private WorkflowStage stage;

    private StageStatus stageStatus;

    @Builder.Default
    private List<String> inputArtifactIds = new ArrayList<>();

    @Builder.Default
    private List<String> outputArtifactIds = new ArrayList<>();

    @Builder.Default
    private List<String> toolCallSummaries = new ArrayList<>();

    @Builder.Default
    private List<String> mcpCallSummaries = new ArrayList<>();

    @Builder.Default
    private List<String> a2aCallSummaries = new ArrayList<>();

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
