package com.yali.mactav.model.verification;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationItem {

    private String itemId;
    private String name;
    private String type;
    private String expected;
    private String actual;
    private Boolean passed;
    private String relatedIntentRelationId;
    private List<String> relatedPlanElementIds;
    private List<String> relatedConfigBlockIds;
    private String relatedTestId;
    private String message;
}
