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
public class DeviceConfig {

    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String vendor;
    private String configText;
    private List<CommandBlock> commandBlocks;
}
