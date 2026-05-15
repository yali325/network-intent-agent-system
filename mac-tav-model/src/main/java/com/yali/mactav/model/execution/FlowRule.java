package com.yali.mactav.model.execution;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlowRule {

    private String ruleId;
    private String deviceId;
    private Integer tableId;
    private Integer priority;
    private Map<String, String> match;
    private List<String> actions;
}
