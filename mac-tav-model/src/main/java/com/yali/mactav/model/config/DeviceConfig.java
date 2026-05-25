package com.yali.mactav.model.config;

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
public class DeviceConfig {

    private String deviceId;

    private String deviceName;

    private String deviceType;

    private String vendor;

    private String configText;

    @Builder.Default
    private List<CommandBlock> commandBlocks = new ArrayList<>();
}
