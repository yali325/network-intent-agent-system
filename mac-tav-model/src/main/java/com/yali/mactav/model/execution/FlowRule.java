package com.yali.mactav.model.execution;

import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowRule {

    private String ruleId;

    private String switchId;

    private String dpid;

    private Integer priority;

    @Builder.Default
    private Map<String, Object> match = new HashMap<>();

    @Builder.Default
    private List<String> actions = new ArrayList<>();

    private TraceRefs traceRefs;
}
