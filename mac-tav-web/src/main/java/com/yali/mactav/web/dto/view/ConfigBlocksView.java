package com.yali.mactav.web.dto.view;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Read-only command block view derived from the current ConfigSet artifact.
 */
@Data
@Builder
public class ConfigBlocksView {

    private String taskId;

    private String status;

    private boolean ready;

    private String sourceArtifactId;

    private Integer configVersion;

    private List<ConfigDeviceView> devices;

    private List<CommandBlockView> commandBlocks;

    private List<String> warnings;

    private String reasonCode;

    private String message;

    /**
     * Device-level config summary for the command panel.
     */
    @Data
    @Builder
    public static class ConfigDeviceView {

        private String deviceId;

        private String deviceName;

        private String vendor;

        private String model;

        private String status;

        private List<String> commands;

        private List<String> rollbackCommands;

        private String summary;

        private String artifactId;

        private Object traceRefs;
    }

    /**
     * Flattened command block for frontend rendering.
     */
    @Data
    @Builder
    public static class CommandBlockView {

        private String deviceId;

        private String deviceName;

        private String blockId;

        private String title;

        private String blockType;

        private Integer order;

        private List<String> commands;

        private List<String> rollbackCommands;

        private String summary;

        private String artifactId;

        private Object traceRefs;
    }
}
