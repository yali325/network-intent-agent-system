package com.yali.mactav.model.config;

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
public class EndpointConfig {

    private String nodeId;

    private String nodeType;

    private String zoneId;

    @Builder.Default
    private List<String> commands = new ArrayList<>();

    private String explanation;

    private TraceRefs traceRefs;
}
