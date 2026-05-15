package com.yali.mactav.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopologyNode {

    private String id;
    private String name;
    private String nodeType;
    private String deviceType;
    private String hostType;
    private String role;
    private String vendor;
    private String zoneId;
}
