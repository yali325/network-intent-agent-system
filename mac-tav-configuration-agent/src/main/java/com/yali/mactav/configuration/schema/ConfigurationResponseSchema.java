package com.yali.mactav.configuration.schema;

import com.yali.mactav.model.config.GenerationSourceType;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured model-output boundary for MAC-TAV ConfigurationAgent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationResponseSchema {

    private String taskId;

    private Integer planVersion;

    private Integer configVersion;

    private TargetEnvironmentSchema targetEnvironment;

    private String generationSummary;

    @Builder.Default
    private List<GenerationSourceSchema> generationSources = new ArrayList<>();

    @Builder.Default
    private List<DeviceConfigSchema> deviceConfigs = new ArrayList<>();

    @Builder.Default
    private List<EndpointConfigSchema> endpointConfigs = new ArrayList<>();

    private RollbackPlanSchema rollbackPlan;

    @Builder.Default
    private List<ConfigWarningSchema> warnings = new ArrayList<>();

    private TraceRefsSchema traceRefs;

    /**
     * Schema for target environment hints needed by configuration generation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TargetEnvironmentSchema {

        private String vendor;

        private String configStyle;

        private String simulationTarget;

        private String adapterType;
    }

    /**
     * Schema for one generation source used as configuration evidence.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationSourceSchema {

        private GenerationSourceType sourceType;

        private String sourceId;

        private String sourceDescription;

        private String sourceName;

        private String description;

        private String artifactRef;

        private Double confidence;
    }

    /**
     * Schema for one device-scoped configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceConfigSchema {

        private String deviceId;

        private String deviceName;

        private String deviceType;

        private String vendor;

        @Builder.Default
        private List<CommandBlockSchema> commandBlocks = new ArrayList<>();

        private EndpointConfigSchema endpointConfig;

        private TraceRefsSchema traceRefs;
    }

    /**
     * Schema for one ordered command block.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandBlockSchema {

        private String blockId;

        private String blockType;

        private Integer order;

        private String title;

        @Builder.Default
        private List<String> commands = new ArrayList<>();

        private String explanation;

        @Builder.Default
        private List<String> rollbackCommands = new ArrayList<>();

        private String rollbackStrategy;

        @Builder.Default
        private List<String> dependsOn = new ArrayList<>();

        private TraceRefsSchema traceRefs;

        private String riskLevel;

        private Boolean isIdempotent;
    }

    /**
     * Schema for endpoint-level configuration details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointConfigSchema {

        private String nodeId;

        private String nodeType;

        private String zoneId;

        @Builder.Default
        private List<String> commands = new ArrayList<>();

        private String explanation;

        private TraceRefsSchema traceRefs;
    }

    /**
     * Schema for rollback blocks tied to generated command blocks.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackPlanSchema {

        private String strategy;

        @Builder.Default
        private List<RollbackBlockSchema> rollbackBlocks = new ArrayList<>();

        private String description;
    }

    /**
     * Schema for one rollback command block.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RollbackBlockSchema {

        private String deviceId;

        private String blockId;

        @Builder.Default
        private List<String> commands = new ArrayList<>();

        private Integer order;
    }

    /**
     * Schema for non-blocking configuration generation warnings.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigWarningSchema {

        private String level;

        private String message;

        private String relatedBlockId;
    }

    /**
     * Schema for cross-stage trace references.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraceRefsSchema {

        @Builder.Default
        private List<String> intentNodeIds = new ArrayList<>();

        @Builder.Default
        private List<String> intentRelationIds = new ArrayList<>();

        @Builder.Default
        private List<String> planElementIds = new ArrayList<>();

        @Builder.Default
        private List<String> configBlockIds = new ArrayList<>();

        @Builder.Default
        private List<String> testIds = new ArrayList<>();

        @Builder.Default
        private List<String> validationItemIds = new ArrayList<>();

        @Builder.Default
        private List<String> repairActionIds = new ArrayList<>();
    }
}
