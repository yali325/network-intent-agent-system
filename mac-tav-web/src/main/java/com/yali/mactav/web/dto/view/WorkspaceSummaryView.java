package com.yali.mactav.web.dto.view;

import com.yali.mactav.model.workflow.job.WorkflowJob;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Read-only workspace summary tailored for the real frontend mission header.
 */
@Data
@Builder
public class WorkspaceSummaryView {

    private String taskId;

    private String taskStatus;

    private String currentStage;

    private String workspaceStatus;

    private Map<String, String> currentArtifactRefs;

    private WorkflowJob latestJob;

    private List<StageCard> stageCards;

    private String currentStageSummary;

    private ViewReadiness readiness;

    private List<String> missingArtifacts;

    private List<String> errors;

    /**
     * One workflow stage card derived from current workspace and artifact state.
     */
    @Data
    @Builder
    public static class StageCard {

        private String stage;

        private String title;

        private String agentName;

        private String status;

        private String artifactType;

        private String artifactId;

        private Integer artifactVersion;

        private String summary;

        private String errorCode;

        private String errorMessage;

        private String updateTime;
    }
}
