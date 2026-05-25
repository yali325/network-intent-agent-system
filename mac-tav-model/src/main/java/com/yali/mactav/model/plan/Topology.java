package com.yali.mactav.model.plan;

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
public class Topology {

    @Builder.Default
    private List<TopologyNode> nodes = new ArrayList<>();

    @Builder.Default
    private List<TopologyLink> links = new ArrayList<>();
}
