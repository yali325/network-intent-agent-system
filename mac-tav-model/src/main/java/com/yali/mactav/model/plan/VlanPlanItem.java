package com.yali.mactav.model.plan;

import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VlanPlanItem {

    private String id;

    private Integer vlanId;

    private String name;

    private String zoneId;

    @Builder.Default
    private List<PortRef> accessPorts = new ArrayList<>();

    @Builder.Default
    private List<PortRef> trunkPorts = new ArrayList<>();

    private TraceRefs traceRefs;
}
