package com.yali.mactav.model.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopologyLink {

    private String id;
    private String sourceNode;
    private String sourceInterface;
    private String targetNode;
    private String targetInterface;
    private String linkType;
}
