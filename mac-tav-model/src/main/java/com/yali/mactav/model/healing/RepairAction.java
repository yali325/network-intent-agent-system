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
public class RepairAction {

    private String actionId;
    private String actionType;
    private String description;
    private List<String> relatedConfigBlockIds;
    private List<String> relatedPlanElementIds;
}
