package com.yali.mactav.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityPolicyPlanItem {

    private String id;
    private String name;
    private String sourceZone;
    private String targetZone;
    private String action;
    private String service;
    private EnforcementPoint enforcementPoint;
    private String basedOnIntentRelation;
}
