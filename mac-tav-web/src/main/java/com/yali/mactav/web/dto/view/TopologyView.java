package com.yali.mactav.web.dto.view;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Read-only topology view derived from NetworkPlan or NetworkIntent artifacts.
 */
@Data
@Builder
public class TopologyView {

    private String taskId;

    private String status;

    private boolean ready;

    private String sourceArtifactType;

    private String sourceArtifactId;

    private String sourceStage;

    private List<TopologyDevice> devices;

    private List<TopologyLink> links;

    private List<String> policies;

    private List<String> annotations;

    private String reasonCode;

    private String message;

    /**
     * Device node used by the real topology board.
     */
    @Data
    @Builder
    public static class TopologyDevice {

        private String id;

        private String name;

        private String role;

        private String zone;

        private String ip;

        private String status;

        private String vendor;

        private String deviceType;

        private Integer x;

        private Integer y;
    }

    /**
     * Link shown between topology devices.
     */
    @Data
    @Builder
    public static class TopologyLink {

        private String id;

        private String from;

        private String to;

        private String label;

        private String status;

        private String policy;

        private String evidence;

        private String sourcePort;

        private String targetPort;
    }
}
