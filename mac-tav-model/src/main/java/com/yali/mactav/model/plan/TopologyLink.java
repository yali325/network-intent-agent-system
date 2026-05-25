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
public class TopologyLink {

    private String id;

    private String sourceNode;

    private String sourceInterface;

    private String targetNode;

    private String targetInterface;

    private String linkType;

    private TraceRefs traceRefs;
}
