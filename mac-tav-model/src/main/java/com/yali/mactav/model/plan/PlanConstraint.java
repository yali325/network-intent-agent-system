package com.yali.mactav.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanConstraint {

    private String id;

    private String type;

    private String description;

    private String sourceIntentId;
}
