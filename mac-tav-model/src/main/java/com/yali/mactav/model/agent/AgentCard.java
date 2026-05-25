package com.yali.mactav.model.agent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public capability descriptor for a professional MAC-TAV agent service.
 *
 * <p>AgentCard is a shared contract in mac-tav-model. It describes discovery
 * metadata and I/O contracts only; it must not contain service registry clients,
 * SDK objects, credentials, or concrete agent beans.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCard {

    private String agentName;

    private String description;

    @Builder.Default
    private List<AgentCapability> capabilities = new ArrayList<>();

    private AgentContract inputContract;

    private AgentContract outputContract;

    private String serviceEndpoint;

    private String protocol;

    private String version;

    private AgentHealthStatus healthStatus;

    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
