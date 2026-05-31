package com.yali.mactav.configuration.validator;

import com.yali.mactav.agent.core.validator.AgentOutputValidator;
import com.yali.mactav.agent.core.validator.ValidationResult;
import com.yali.mactav.model.config.CommandBlock;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigWarning;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.config.GenerationSource;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates ConfigSet output before it can leave mac-tav-configuration-agent.
 */
public class ConfigurationOutputValidator implements AgentOutputValidator<ConfigSet> {

    private static final Pattern NON_STRUCTURED_CONFIG_PATTERN =
            Pattern.compile("\\b(configText|running-config|interface\\s+\\S+|vlan\\s+\\d+|router\\s+ospf|"
                    + "access-list|ip\\s+address|switchport\\s+mode)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern DOWNSTREAM_BOUNDARY_PATTERN =
            Pattern.compile("(已执行配置|已下发命令|验证通过|已修复|execution\\s+completed|"
                    + "configuration\\s+applied|validation\\s+passed|repaired)",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public ValidationResult validate(ConfigSet output) {
        List<String> messages = new ArrayList<>();
        if (output == null) {
            messages.add("ConfigSet must not be null");
            return ValidationResult.fail(messages);
        }

        requireNotBlank(messages, "taskId", output.getTaskId());
        requireNotNull(messages, "planVersion", output.getPlanVersion());
        requireNotNull(messages, "configVersion", output.getConfigVersion());
        validateGenerationSources(messages, output.getGenerationSources());
        validateDeviceConfigs(messages, output.getDeviceConfigs());
        validateTraceRefs(messages, "traceRefs", output.getTraceRefs(), false);
        rejectBoundaryContent(messages, "generationSummary", output.getGenerationSummary());
        validateWarnings(messages, output.getWarnings());

        return messages.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(messages);
    }

    private void validateGenerationSources(List<String> messages, List<GenerationSource> sources) {
        if (sources == null || sources.isEmpty()) {
            messages.add("generationSources must not be empty");
            return;
        }
        for (GenerationSource source : sources) {
            if (source == null) {
                messages.add("generationSource must not be null");
                continue;
            }
            if (source.getSourceType() == null) {
                messages.add("generationSource.sourceType must not be null");
            }
            rejectDownstreamBoundaryContent(messages, "generationSource.sourceDescription", source.getSourceDescription());
            rejectDownstreamBoundaryContent(messages, "generationSource.description", source.getDescription());
        }
    }

    private void validateDeviceConfigs(List<String> messages, List<DeviceConfig> devices) {
        if (devices == null || devices.isEmpty()) {
            messages.add("deviceConfigs must not be empty");
            return;
        }
        for (DeviceConfig device : devices) {
            if (device == null) {
                messages.add("deviceConfig must not be null");
                continue;
            }
            requireNotBlank(messages, "deviceConfig.deviceName", device.getDeviceName());
            requireNotBlank(messages, "deviceConfig.deviceType", device.getDeviceType());
            validateCommandBlocks(messages, device.getDeviceName(), device.getCommandBlocks());
            validateTraceRefs(messages, "deviceConfig.traceRefs", device.getTraceRefs(), false);
        }
    }

    private void validateCommandBlocks(List<String> messages, String deviceName, List<CommandBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            messages.add("deviceConfig.commandBlocks must not be empty for device: " + deviceName);
            return;
        }
        for (CommandBlock block : blocks) {
            if (block == null) {
                messages.add("commandBlock must not be null");
                continue;
            }
            requireNotBlank(messages, "commandBlock.blockId", block.getBlockId());
            requireNotBlank(messages, "commandBlock.explanation", block.getExplanation());
            if (block.getCommands() == null || block.getCommands().isEmpty()) {
                messages.add("commandBlock.commands must not be empty: " + block.getBlockId());
            }
            else {
                for (String command : block.getCommands()) {
                    requireNotBlank(messages, "commandBlock.commands[]", command);
                    rejectDownstreamBoundaryContent(messages, "commandBlock.commands[]", command);
                }
            }
            if (block.getRollbackCommands() == null || block.getRollbackCommands().isEmpty()) {
                messages.add("commandBlock.rollbackCommands must not be empty: " + block.getBlockId());
            }
            validateTraceRefs(messages, "commandBlock.traceRefs", block.getTraceRefs(), true);
            rejectDownstreamBoundaryContent(messages, "commandBlock.explanation", block.getExplanation());
        }
    }

    private void validateTraceRefs(List<String> messages, String fieldName, TraceRefs traceRefs, boolean requirePlanOrIntent) {
        if (traceRefs == null) {
            messages.add(fieldName + " must not be null");
            return;
        }
        if (requirePlanOrIntent && isEmpty(traceRefs.getPlanElementIds()) && isEmpty(traceRefs.getIntentRelationIds())) {
            messages.add(fieldName + " must include planElementIds or intentRelationIds");
        }
    }

    private void validateWarnings(List<String> messages, List<ConfigWarning> warnings) {
        if (warnings == null) {
            return;
        }
        for (ConfigWarning warning : warnings) {
            if (warning != null) {
                rejectDownstreamBoundaryContent(messages, "warning.message", warning.getMessage());
            }
        }
    }

    private void rejectBoundaryContent(List<String> messages, String fieldName, String value) {
        if (isBlank(value)) {
            return;
        }
        if (NON_STRUCTURED_CONFIG_PATTERN.matcher(value).find()) {
            messages.add(fieldName + " contains non-structured config text");
        }
        if (DOWNSTREAM_BOUNDARY_PATTERN.matcher(value).find()) {
            messages.add(fieldName + " contains execution, verification, or repair boundary content");
        }
    }

    private void rejectDownstreamBoundaryContent(List<String> messages, String fieldName, String value) {
        if (isBlank(value)) {
            return;
        }
        if (DOWNSTREAM_BOUNDARY_PATTERN.matcher(value).find()) {
            messages.add(fieldName + " contains execution, verification, or repair boundary content");
        }
    }

    private void requireNotBlank(List<String> messages, String fieldName, String value) {
        if (isBlank(value)) {
            messages.add(fieldName + " must not be blank");
        }
    }

    private void requireNotNull(List<String> messages, String fieldName, Object value) {
        if (value == null) {
            messages.add(fieldName + " must not be null");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isEmpty(List<String> values) {
        return values == null || values.isEmpty();
    }
}
