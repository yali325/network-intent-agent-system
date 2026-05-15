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
public class RoutingPlan {

    private String id;
    private String protocol;
    private String area;
    private List<RoutingRouterItem> routers;
    private DefaultRoute defaultRoute;
}
