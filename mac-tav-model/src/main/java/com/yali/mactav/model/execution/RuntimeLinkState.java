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
public class RuntimeLinkState {

    private String linkId;

    private String status;

    private String sourceNode;

    private String targetNode;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
