package com.yali.mactav.model.config;

import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.plan.TargetEnvironment;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Config 阶段产物对象
 */
/**
 * Configuration-stage artifact containing structured device and endpoint configuration.
 *
 * <p>ConfigSet is not a raw command blob. It should remain serializable DTO data
 * and must not execute configuration or judge verification success.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigSet {

    private String taskId;

    private Integer planVersion;

    private Integer configVersion;

    private TargetEnvironment targetEnvironment;

    private String generationSummary;

    @Builder.Default
    private List<GenerationSource> generationSources = new ArrayList<>();

    @Builder.Default
    private List<DeviceConfig> deviceConfigs = new ArrayList<>();

    @Builder.Default
    private List<EndpointConfig> endpointConfigs = new ArrayList<>();

    private RollbackPlan rollbackPlan;

    @Builder.Default
    private List<ConfigWarning> warnings = new ArrayList<>();

    private StageStatus stageStatus;

    private TraceRefs traceRefs;

    private LocalDateTime createTime;
}
