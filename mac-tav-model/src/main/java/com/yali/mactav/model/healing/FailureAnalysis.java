package com.yali.mactav.model.healing;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureAnalysis {

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
