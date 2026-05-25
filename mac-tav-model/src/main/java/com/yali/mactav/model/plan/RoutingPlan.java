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
public class RoutingPlan {

    private String id;

    private String protocol;

    private String area;

    @Builder.Default
    private List<RoutingRouter> routers = new ArrayList<>();

    private DefaultRoute defaultRoute;

    private TraceRefs traceRefs;
}
