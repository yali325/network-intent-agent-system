package com.yali.mactav.model.execution;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeNodeState {

    private String nodeId;

    private String status;

    @Builder.Default
    private Map<String, Object> interfaces = new HashMap<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
