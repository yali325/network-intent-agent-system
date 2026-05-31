package com.yali.mactav.configuration.parser;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema;
import com.yali.mactav.configuration.schema.ConfigurationResponseSchema.*;
import com.yali.mactav.model.config.*;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.plan.TargetEnvironment;
import com.yali.mactav.model.workspace.TraceRefs;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts ConfigurationResponseSchema into the shared ConfigSet DTO.
 */
public class ConfigurationResponseParser implements AgentResponseParser<ConfigurationResponseSchema, ConfigSet> {

    private static final String DEFAULT_CREATED_BY = "ConfigurationAgent";

    @Override
    public ConfigSet parse(ConfigurationResponseSchema schema, AgentRunContext context) {
        ConfigurationResponseSchema safeSchema = schema == null ? new ConfigurationResponseSchema() : schema;
        LocalDateTime now = LocalDateTime.now();

        return ConfigSet.builder()
                .taskId(resolveTaskId(safeSchema, context))
                .planVersion(safeSchema.getPlanVersion())
                .configVersion(resolveConfigVersion(safeSchema, context))
                .targetEnvironment(mapTargetEnvironment(safeSchema.getTargetEnvironment()))
                .generationSummary(safeSchema.getGenerationSummary())
                .generationSources(mapGenerationSources(safeSchema.getGenerationSources()))
                .deviceConfigs(mapDeviceConfigs(safeSchema.getDeviceConfigs()))
                .endpointConfigs(mapEndpointConfigs(safeSchema.getEndpointConfigs()))
                .rollbackPlan(mapRollbackPlan(safeSchema.getRollbackPlan()))
                .warnings(mapWarnings(safeSchema.getWarnings()))
                .stageStatus(StageStatus.SUCCESS)
                .traceRefs(mapTraceRefs(safeSchema.getTraceRefs()))
                .createTime(now)
                .updateTime(now)
                .createdBy(resolveCreatedBy(context))
                .build();
    }

    private String resolveTaskId(ConfigurationResponseSchema schema, AgentRunContext context) {
        if (context != null && !isBlank(context.getTaskId())) {
            return context.getTaskId();
        }
        return schema.getTaskId();
    }

    private Integer resolveConfigVersion(ConfigurationResponseSchema schema, AgentRunContext context) {
        if (context != null && context.getVersion() != null) {
            return context.getVersion();
        }
        return schema.getConfigVersion();
    }

    private String resolveCreatedBy(AgentRunContext context) {
        if (context != null && !isBlank(context.getCreatedBy())) {
            return context.getCreatedBy();
        }
        return DEFAULT_CREATED_BY;
    }

    private TargetEnvironment mapTargetEnvironment(TargetEnvironmentSchema schema) {
        if (schema == null) {
            return null;
        }
        return TargetEnvironment.builder()
                .vendor(schema.getVendor())
                .configStyle(schema.getConfigStyle())
                .simulationTarget(schema.getSimulationTarget())
                .adapterType(schema.getAdapterType())
                .build();
    }

    private List<GenerationSource> mapGenerationSources(List<GenerationSourceSchema> schemas) {
        List<GenerationSource> sources = new ArrayList<>();
        for (GenerationSourceSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            sources.add(GenerationSource.builder()
                    .sourceType(schema.getSourceType())
                    .sourceId(schema.getSourceId())
                    .sourceDescription(schema.getSourceDescription())
                    .sourceName(schema.getSourceName())
                    .description(schema.getDescription())
                    .artifactRef(schema.getArtifactRef())
                    .confidence(schema.getConfidence())
                    .build());
        }
        return sources;
    }

    private List<DeviceConfig> mapDeviceConfigs(List<DeviceConfigSchema> schemas) {
        List<DeviceConfig> devices = new ArrayList<>();
        for (DeviceConfigSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            devices.add(DeviceConfig.builder()
                    .deviceId(schema.getDeviceId())
                    .deviceName(schema.getDeviceName())
                    .deviceType(schema.getDeviceType())
                    .vendor(schema.getVendor())
                    .commandBlocks(mapCommandBlocks(schema.getCommandBlocks()))
                    .endpointConfig(mapEndpointConfig(schema.getEndpointConfig()))
                    .traceRefs(mapTraceRefs(schema.getTraceRefs()))
                    .build());
        }
        return devices;
    }

    private List<CommandBlock> mapCommandBlocks(List<CommandBlockSchema> schemas) {
        List<CommandBlock> blocks = new ArrayList<>();
        for (CommandBlockSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            blocks.add(CommandBlock.builder()
                    .blockId(schema.getBlockId())
                    .blockType(schema.getBlockType())
                    .order(schema.getOrder())
                    .title(schema.getTitle())
                    .commands(safeList(schema.getCommands()))
                    .explanation(schema.getExplanation())
                    .rollbackCommands(safeList(schema.getRollbackCommands()))
                    .rollbackStrategy(schema.getRollbackStrategy())
                    .dependsOn(safeList(schema.getDependsOn()))
                    .traceRefs(mapTraceRefs(schema.getTraceRefs()))
                    .riskLevel(schema.getRiskLevel())
                    .isIdempotent(schema.getIsIdempotent())
                    .build());
        }
        return blocks;
    }

    private List<EndpointConfig> mapEndpointConfigs(List<EndpointConfigSchema> schemas) {
        List<EndpointConfig> endpoints = new ArrayList<>();
        for (EndpointConfigSchema schema : safeList(schemas)) {
            if (schema != null) {
                endpoints.add(mapEndpointConfig(schema));
            }
        }
        return endpoints;
    }

    private EndpointConfig mapEndpointConfig(EndpointConfigSchema schema) {
        if (schema == null) {
            return null;
        }
        return EndpointConfig.builder()
                .nodeId(schema.getNodeId())
                .nodeType(schema.getNodeType())
                .zoneId(schema.getZoneId())
                .commands(safeList(schema.getCommands()))
                .explanation(schema.getExplanation())
                .traceRefs(mapTraceRefs(schema.getTraceRefs()))
                .build();
    }

    private RollbackPlan mapRollbackPlan(RollbackPlanSchema schema) {
        if (schema == null) {
            return null;
        }
        List<RollbackBlock> blocks = new ArrayList<>();
        for (RollbackBlockSchema block : safeList(schema.getRollbackBlocks())) {
            if (block == null) {
                continue;
            }
            blocks.add(RollbackBlock.builder()
                    .deviceId(block.getDeviceId())
                    .blockId(block.getBlockId())
                    .commands(safeList(block.getCommands()))
                    .order(block.getOrder())
                    .build());
        }
        return RollbackPlan.builder()
                .strategy(schema.getStrategy())
                .rollbackBlocks(blocks)
                .description(schema.getDescription())
                .build();
    }

    private List<ConfigWarning> mapWarnings(List<ConfigWarningSchema> schemas) {
        List<ConfigWarning> warnings = new ArrayList<>();
        for (ConfigWarningSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            warnings.add(ConfigWarning.builder()
                    .level(schema.getLevel())
                    .message(schema.getMessage())
                    .relatedBlockId(schema.getRelatedBlockId())
                    .build());
        }
        return warnings;
    }

    private TraceRefs mapTraceRefs(TraceRefsSchema schema) {
        if (schema == null) {
            return null;
        }
        return TraceRefs.builder()
                .intentNodeIds(safeList(schema.getIntentNodeIds()))
                .intentRelationIds(safeList(schema.getIntentRelationIds()))
                .planElementIds(safeList(schema.getPlanElementIds()))
                .configBlockIds(safeList(schema.getConfigBlockIds()))
                .testIds(safeList(schema.getTestIds()))
                .validationItemIds(safeList(schema.getValidationItemIds()))
                .repairActionIds(safeList(schema.getRepairActionIds()))
                .build();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
