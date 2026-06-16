package com.yali.mactav.configuration.service;

import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import com.yali.mactav.model.config.DeviceConfig;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Normalizes ConfigSet presentation fields before trace stabilization and validation.
 */
public class ConfigurationConfigSetNormalizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationConfigSetNormalizer.class);

    private static final int MAX_SUMMARY_LENGTH = 220;

    private static final Pattern CONFIG_COMMAND_PATTERN = Pattern.compile(
            "(?im)(^\\s*(system-view|interface\\b|vlan\\b|acl\\b|rule\\s+(permit|deny)\\b|"
                    + "ip\\s+route-static\\b|ospf\\b|port\\s+link-type\\b|port\\s+trunk\\s+allow-pass\\b|"
                    + "quit\\b|return\\b|display\\s+current-configuration\\b|access-list\\b|"
                    + "ip\\s+access-list\\b|permit\\s+tcp\\b|deny\\s+ip\\b)|"
                    + "\\b(configText|running-config)\\b)");

    public ConfigSet normalize(ConfigSet configSet, ConfigurationAgentInvokePayload payload) {
        if (configSet == null) {
            return null;
        }
        long start = System.nanoTime();
        int deviceCount = safeList(configSet.getDeviceConfigs()).size();
        int commandBlockCount = commandBlockCount(configSet);
        boolean sanitized = normalizeGenerationSummary(configSet, deviceCount, commandBlockCount);
        LOGGER.info(
                "Configuration ConfigSet normalized taskId={}, traceId={}, summarySanitized={}, deviceCount={}, commandBlockCount={}, durationMs={}",
                configSet.getTaskId(),
                payload == null ? null : payload.getTraceId(),
                sanitized,
                deviceCount,
                commandBlockCount,
                elapsedMillis(start));
        return configSet;
    }

    private boolean normalizeGenerationSummary(ConfigSet configSet, int deviceCount, int commandBlockCount) {
        String summary = configSet.getGenerationSummary();
        if (isBlank(summary) || containsConfigCommand(summary) || tooLong(summary)) {
            configSet.setGenerationSummary(defaultSummary(configSet.getTaskId(), deviceCount, commandBlockCount));
            return true;
        }
        configSet.setGenerationSummary(summary.replaceAll("\\s+", " ").trim());
        return false;
    }

    private boolean containsConfigCommand(String value) {
        return value != null && CONFIG_COMMAND_PATTERN.matcher(value).find();
    }

    private boolean tooLong(String value) {
        return value != null && value.trim().length() > MAX_SUMMARY_LENGTH;
    }

    private String defaultSummary(String taskId, int deviceCount, int commandBlockCount) {
        String safeTaskId = isBlank(taskId) ? "unknown" : taskId;
        return "Generated configuration set for task " + safeTaskId
                + ", devices=" + deviceCount
                + ", commandBlocks=" + commandBlockCount + ".";
    }

    private int commandBlockCount(ConfigSet configSet) {
        int count = 0;
        for (DeviceConfig deviceConfig : safeList(configSet.getDeviceConfigs())) {
            count += safeList(deviceConfig == null ? null : deviceConfig.getCommandBlocks()).size();
        }
        return count;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
