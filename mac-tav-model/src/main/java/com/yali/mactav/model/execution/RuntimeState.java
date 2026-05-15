package com.yali.mactav.model.execution;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuntimeState {

    private String environmentStatus;
    private List<RuntimeNodeState> nodes;
    private List<RuntimeLinkState> links;
    private Boolean controllerConnected;
}
