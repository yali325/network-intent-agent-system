package com.yali.mactav.model.config;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointConfig {

    private String nodeId;
    private String nodeType;
    private String zoneId;
    private List<String> commands;
    private String explanation;
}
