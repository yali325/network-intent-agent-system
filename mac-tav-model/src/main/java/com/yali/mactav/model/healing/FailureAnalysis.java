package com.yali.mactav.model.healing;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureAnalysis {

    private String failureId;
    private String failureType;
    private String reason;
    private List<String> relatedValidationItemIds;
    private List<String> relatedConfigBlockIds;
    private List<String> relatedPlanElementIds;
}
