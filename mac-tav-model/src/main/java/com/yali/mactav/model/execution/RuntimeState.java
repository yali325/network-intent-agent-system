package com.yali.mactav.model.execution;

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
public class RuntimeState {

    private String environmentStatus;

    @Builder.Default
    private List<RuntimeNodeState> nodes = new ArrayList<>();

    @Builder.Default
    private List<RuntimeLinkState> links = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> controllerState = new HashMap<>();

    @Builder.Default
    private Map<String, Object> flowState = new HashMap<>();

    @Builder.Default
    private List<String> rawLogs = new ArrayList<>();
}
