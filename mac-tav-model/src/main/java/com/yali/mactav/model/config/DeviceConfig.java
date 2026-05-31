package com.yali.mactav.model.config;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.yali.mactav.model.workspace.TraceRefs;

/**
 * Device-scoped configuration containing structured command blocks and endpoint settings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceConfig {

    private String deviceId;

    private String deviceName;

    private String deviceType;

    private String vendor;

    @Builder.Default
    private List<CommandBlock> commandBlocks = new ArrayList<>();

    private EndpointConfig endpointConfig;

    private TraceRefs traceRefs;
}
