package com.yali.mactav.model.plan;

import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Planning-stage artifact describing network design decisions.
 *
 * <p>NetworkPlan may describe topology, zones, routing, addressing, and policy
 * intent, but it must not contain executable CLI or mutate workspace state.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkPlan {

    private String planId;

    private String taskId;

    private String intentId;

    private Integer intentVersion;

    private Integer planVersion;

    private String planSummary;

    private SelectedArchitecture selectedArchitecture;

    private TargetEnvironment targetEnvironment;

    private Topology topology;

    @Builder.Default
    private List<NetworkZone> zones = new ArrayList<>();

    @Builder.Default
    private List<AddressPlanItem> addressPlan = new ArrayList<>();

    @Builder.Default
    private List<VlanPlanItem> vlanPlan = new ArrayList<>();

    private RoutingPlan routingPlan;

    @Builder.Default
    private List<SecurityPolicyPlanItem> securityPolicyPlan = new ArrayList<>();

    private NatPlan natPlan;

    @Builder.Default
    private List<PlanConstraint> planConstraints = new ArrayList<>();

    private TraceRefs traceRefs;

    private StageStatus stageStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String createdBy;
}
