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
public class RepairPlan {

    private String repairPlanId;
    private String taskId;
    private String summary;
    private List<RepairAction> actions;
    private FailureAnalysis failureAnalysis;
}
