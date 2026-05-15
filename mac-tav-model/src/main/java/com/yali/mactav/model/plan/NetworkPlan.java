package com.yali.mactav.model.plan;

import com.yali.mactav.common.enums.StageStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkPlan {

    private String taskId;
    private Integer intentVersion;
    private Integer planVersion;
    private String planSummary;
    private SelectedArchitecture selectedArchitecture;
    private Topology topology;
    private List<NetworkZone> zones;
    private List<AddressPlanItem> addressPlan;
    private List<VlanPlanItem> vlanPlan;
    private RoutingPlan routingPlan;
    private List<SecurityPolicyPlanItem> securityPolicyPlan;
    private NatPlan natPlan;
    private TargetEnvironment targetEnvironment;
    private StageStatus stageStatus;
}
