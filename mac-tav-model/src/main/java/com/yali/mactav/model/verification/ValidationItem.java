package com.yali.mactav.model.verification;

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
public class ValidationItem {

    private String itemId;

    private String name;

    private String type;

    private String expected;

    private String actual;

    private Boolean passed;

    private String severity;

    private String relatedIntentRelationId;

    @Builder.Default
    private List<String> relatedPlanElementIds = new ArrayList<>();

    @Builder.Default
    private List<String> relatedConfigBlockIds = new ArrayList<>();

    private String relatedTestId;

    @Builder.Default
    private List<String> evidenceIds = new ArrayList<>();

    private String message;
}
