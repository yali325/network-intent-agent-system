package com.yali.mactav.model.config;

import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.model.plan.TargetEnvironment;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigSet {

    private String taskId;
    private Integer planVersion;
    private Integer configVersion;
    private TargetEnvironment targetEnvironment;
    private String generationSummary;
    private List<GenerationSources> generationSources;
    private List<DeviceConfig> deviceConfigs;
    private List<EndpointConfig> endpointConfigs;
    private RollbackPlan rollbackPlan;
    private List<ConfigWarning> warnings;
    private StageStatus stageStatus;
}
