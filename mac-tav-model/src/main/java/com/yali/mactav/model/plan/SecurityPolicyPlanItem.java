package com.yali.mactav.model.plan;

import com.yali.mactav.model.workspace.TraceRefs;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityPolicyPlanItem {

    private String id;

    private String name;

    private String sourceZone;

    private String targetZone;

    private String action;

    private String service;

    private EnforcementPoint enforcementPoint;

    private String basedOnIntentRelation;

    private TraceRefs traceRefs;
}
