package com.yali.mactav.model.plan;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VlanPlanItem {

    private String id;
    private Integer vlanId;
    private String name;
    private String zoneId;
    private List<PortRef> accessPorts;
}
