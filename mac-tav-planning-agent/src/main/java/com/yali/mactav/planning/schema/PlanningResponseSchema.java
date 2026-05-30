package com.yali.mactav.planning.schema;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured model-output boundary for MAC-TAV PlanningAgent.
 *
 * <p>This schema is parsed into NetworkPlan before validation. It must only
 * describe network design decisions: topology, zones, addressing, VLANs,
 * routing, and security policies. It must not contain executable CLI or
 * device-specific configuration blocks.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningResponseSchema {

    private String planSummary;

    private ArchitectureSchema selectedArchitecture;

    private TargetEnvironmentSchema targetEnvironment;

    @Builder.Default
    private List<TopologyNodeSchema> topologyNodes = new ArrayList<>();

    @Builder.Default
    private List<TopologyLinkSchema> topologyLinks = new ArrayList<>();

    @Builder.Default
    private List<ZoneSchema> zones = new ArrayList<>();

    @Builder.Default
    private List<AddressPlanItemSchema> addressPlan = new ArrayList<>();

    @Builder.Default
    private List<VlanPlanItemSchema> vlanPlan = new ArrayList<>();

    private RoutingPlanSchema routingPlan;

    @Builder.Default
    private List<SecurityPolicySchema> securityPolicies = new ArrayList<>();

    private NatPlanSchema natPlan;

    @Builder.Default
    private List<PlanConstraintSchema> planConstraints = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Schema for selected architecture decision.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchitectureSchema {

        private String id;

        private String type;

        private String reason;

        @Builder.Default
        private List<String> tradeoffs = new ArrayList<>();
    }

    /**
     * Schema for target execution environment hints.
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
     * Schema for one topology node.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopologyNodeSchema {

        private String id;

        private String name;

        private String nodeType;

        private String deviceType;

        private String hostType;

        private String role;

        private String vendor;

        private String zoneId;

        @Builder.Default
        private List<String> traceIntentNodeIds = new ArrayList<>();
    }

    /**
     * Schema for one topology link.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopologyLinkSchema {

        private String id;

        private String sourceNode;

        private String sourceInterface;

        private String targetNode;

        private String targetInterface;

        private String linkType;

        @Builder.Default
        private List<String> traceIntentNodeIds = new ArrayList<>();
    }

    /**
     * Schema for one network zone.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneSchema {

        private String id;

        private String name;

        private String mappedFromIntentNode;

        private String zoneType;

        private String description;
    }

    /**
     * Schema for one address plan entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressPlanItemSchema {

        private String id;

        private String zoneId;

        private String subnet;

        private String gateway;

        @Builder.Default
        private List<String> dnsServers = new ArrayList<>();

        private String exampleHostAddress;

        @Builder.Default
        private List<String> hostAddressHints = new ArrayList<>();

        @Builder.Default
        private List<String> traceIntentNodeIds = new ArrayList<>();
    }

    /**
     * Schema for one VLAN plan entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VlanPlanItemSchema {

        private String id;

        private Integer vlanId;

        private String name;

        private String zoneId;

        @Builder.Default
        private List<PortRefSchema> accessPorts = new ArrayList<>();

        @Builder.Default
        private List<PortRefSchema> trunkPorts = new ArrayList<>();

        @Builder.Default
        private List<String> traceIntentNodeIds = new ArrayList<>();
    }

    /**
     * Schema for a port reference used in VLAN plans.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortRefSchema {

        private String deviceId;

        private String interfaceName;

        private String description;
    }

    /**
     * Schema for the routing plan.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutingPlanSchema {

        private String id;

        private String protocol;

        private String area;

        @Builder.Default
        private List<RoutingRouterSchema> routers = new ArrayList<>();

        private DefaultRouteSchema defaultRoute;

        @Builder.Default
        private List<String> traceIntentNodeIds = new ArrayList<>();
    }

    /**
     * Schema for one routing router.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoutingRouterSchema {

        private String id;

        private String deviceId;

        private String routerId;

        @Builder.Default
        private List<String> advertisedNetworks = new ArrayList<>();

        @Builder.Default
        private List<String> traceIntentNodeIds = new ArrayList<>();
    }

    /**
     * Schema for default route configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultRouteSchema {

        private Boolean enabled;

        private String nextHop;

        private String outInterface;
    }

    /**
     * Schema for one security policy.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityPolicySchema {

        private String id;

        private String name;

        private String sourceZone;

        private String targetZone;

        private String action;

        private String service;

        private String enforcementDeviceId;

        private String enforcementInterfaceName;

        private String enforcementDirection;

        private String basedOnIntentRelation;

        @Builder.Default
        private List<String> traceIntentNodeIds = new ArrayList<>();

        @Builder.Default
        private List<String> traceIntentRelationIds = new ArrayList<>();
    }

    /**
     * Schema for NAT plan.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NatPlanSchema {

        private String id;

        private Boolean enabled;

        @Builder.Default
        private List<String> insideZones = new ArrayList<>();

        private PortRefSchema outsideInterface;

        private String description;

        @Builder.Default
        private List<String> traceIntentNodeIds = new ArrayList<>();
    }

    /**
     * Schema for a plan-level constraint.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanConstraintSchema {

        private String id;

        private String type;

        private String description;

        private String sourceIntentId;
    }
}
