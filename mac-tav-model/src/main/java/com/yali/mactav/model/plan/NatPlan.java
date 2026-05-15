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
public class NatPlan {

    private String id;
    private Boolean enabled;
    private List<String> insideZones;
    private PortRef outsideInterface;
    private String description;
}
