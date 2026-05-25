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
public class NatPlan {

    private String id;

    private Boolean enabled;

    @Builder.Default
    private List<String> insideZones = new ArrayList<>();

    private PortRef outsideInterface;

    private String description;

    private TraceRefs traceRefs;
}
