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
public class RoutingRouterItem {

    private String id;
    private String deviceId;
    private String routerId;
    private List<String> advertisedNetworks;
}
