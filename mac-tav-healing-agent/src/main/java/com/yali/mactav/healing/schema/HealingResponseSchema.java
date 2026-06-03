package com.yali.mactav.healing.schema;

import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.enums.WorkflowStage;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured model-output boundary for MAC-TAV HealingAgent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealingResponseSchema {

    private String overallRepairStrategy;

    private Boolean requiresUserConfirmation;

    @Builder.Default
    private List<FailureAnalysisSchema> failureAnalysis = new ArrayList<>();

    @Builder.Default
    private List<RepairActionSchema> actions = new ArrayList<>();

    /**
     * Schema representation of one validation failure diagnosis.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureAnalysisSchema {

        private String analysisId;

        private String failureType;

        private String rootCauseSummary;

        @Builder.Default
        private List<String> relatedValidationItemIds = new ArrayList<>();

        @Builder.Default
        private List<String> relatedIntentRelationIds = new ArrayList<>();

        @Builder.Default
        private List<String> relatedPlanElementIds = new ArrayList<>();

        @Builder.Default
        private List<String> relatedConfigBlockIds = new ArrayList<>();

        private Double confidence;

        @Builder.Default
        private List<String> evidenceIds = new ArrayList<>();
    }

    /**
     * Schema representation of one proposed repair action.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepairActionSchema {

        private String actionId;

        private String actionType;

        private WorkflowStage targetStage;

        private String description;

        private String relatedFailureAnalysisId;

        @Builder.Default
        private List<String> relatedValidationItemIds = new ArrayList<>();

        @Builder.Default
        private List<String> relatedIntentRelationIds = new ArrayList<>();

        @Builder.Default
        private List<String> relatedPlanElementIds = new ArrayList<>();

        @Builder.Default
        private List<String> relatedConfigBlockIds = new ArrayList<>();

        @Builder.Default
        private List<String> inputArtifactIds = new ArrayList<>();

        private ArtifactType expectedOutputArtifactType;

        private String riskLevel;

        private String riskReason;

        private Boolean requiresApproval;
    }
}
