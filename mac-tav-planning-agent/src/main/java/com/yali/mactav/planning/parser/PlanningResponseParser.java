package com.yali.mactav.planning.parser;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.agent.core.parser.AgentResponseParser;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.plan.*;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.planning.schema.PlanningResponseSchema;
import com.yali.mactav.planning.schema.PlanningResponseSchema.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts PlanningResponseSchema into the shared NetworkPlan DTO.
 *
 * <p>This parser belongs to mac-tav-planning-agent. It fills task metadata from
 * AgentRunContext and normalizes null lists, but it must not call models, write
 * NetworkWorkspace, or perform orchestration.</p>
 */
public class PlanningResponseParser implements AgentResponseParser<PlanningResponseSchema, NetworkPlan> {

    /** Default createdBy when context does not carry the field. */
    private static final String DEFAULT_CREATED_BY = "PlanningAgent";

    @Override
    public NetworkPlan parse(PlanningResponseSchema schema, AgentRunContext context) {
        PlanningResponseSchema safeSchema = schema == null ? new PlanningResponseSchema() : schema;
        LocalDateTime now = LocalDateTime.now();

        String createdBy = resolveCreatedBy(context);

        return NetworkPlan.builder()
                .taskId(context == null ? null : context.getTaskId())
                .intentVersion(context == null ? null : context.getVersion())
                .planVersion(context == null ? null : context.getVersion())
                .planSummary(safeSchema.getPlanSummary())
                .selectedArchitecture(mapArchitecture(safeSchema.getSelectedArchitecture()))
                .targetEnvironment(mapTargetEnvironment(safeSchema.getTargetEnvironment()))
                .topology(mapTopology(safeSchema))
                .zones(mapZones(safeSchema.getZones()))
                .addressPlan(mapAddressPlan(safeSchema.getAddressPlan()))
                .vlanPlan(mapVlanPlan(safeSchema.getVlanPlan()))
                .routingPlan(mapRoutingPlan(safeSchema.getRoutingPlan(), safeSchema))
                .securityPolicyPlan(mapSecurityPolicies(safeSchema.getSecurityPolicies()))
                .natPlan(mapNatPlan(safeSchema.getNatPlan()))
                .planConstraints(mapPlanConstraints(safeSchema.getPlanConstraints()))
                .traceRefs(buildTraceRefs(safeSchema))
                .stageStatus(StageStatus.SUCCESS)
                .createTime(now)
                .updateTime(now)
                .createdBy(createdBy)
                .build();
    }

    private String resolveCreatedBy(AgentRunContext context) {
        if (context != null && context.getCreatedBy() != null && !context.getCreatedBy().isBlank()) {
            return context.getCreatedBy();
        }
        return DEFAULT_CREATED_BY;
    }

    private SelectedArchitecture mapArchitecture(ArchitectureSchema schema) {
        if (schema == null) {
            return null;
        }
        return SelectedArchitecture.builder()
                .id(schema.getId())
                .type(schema.getType())
                .reason(schema.getReason())
                .tradeoffs(safeList(schema.getTradeoffs()))
                .build();
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

    private Topology mapTopology(PlanningResponseSchema schema) {
        List<TopologyNode> nodes = new ArrayList<>();
        for (TopologyNodeSchema nodeSchema : safeList(schema.getTopologyNodes())) {
            if (nodeSchema == null) {
                continue;
            }
            nodes.add(TopologyNode.builder()
                    .id(nodeSchema.getId())
                    .name(nodeSchema.getName())
                    .nodeType(nodeSchema.getNodeType())
                    .deviceType(nodeSchema.getDeviceType())
                    .hostType(nodeSchema.getHostType())
                    .role(nodeSchema.getRole())
                    .vendor(nodeSchema.getVendor())
                    .zoneId(nodeSchema.getZoneId())
                    .traceRefs(intentTraceRefs(nodeSchema.getTraceIntentNodeIds()))
                    .build());
        }
        List<TopologyLink> links = new ArrayList<>();
        for (TopologyLinkSchema linkSchema : safeList(schema.getTopologyLinks())) {
            if (linkSchema == null) {
                continue;
            }
            links.add(TopologyLink.builder()
                    .id(linkSchema.getId())
                    .sourceNode(linkSchema.getSourceNode())
                    .sourceInterface(linkSchema.getSourceInterface())
                    .targetNode(linkSchema.getTargetNode())
                    .targetInterface(linkSchema.getTargetInterface())
                    .linkType(linkSchema.getLinkType())
                    .traceRefs(intentTraceRefs(linkSchema.getTraceIntentNodeIds()))
                    .build());
        }
        return Topology.builder().nodes(nodes).links(links).build();
    }

    private List<NetworkZone> mapZones(List<ZoneSchema> schemas) {
        List<NetworkZone> zones = new ArrayList<>();
        for (ZoneSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            zones.add(NetworkZone.builder()
                    .id(schema.getId())
                    .name(schema.getName())
                    .mappedFromIntentNode(schema.getMappedFromIntentNode())
                    .zoneType(schema.getZoneType())
                    .description(schema.getDescription())
                    .build());
        }
        return zones;
    }

    private List<AddressPlanItem> mapAddressPlan(List<AddressPlanItemSchema> schemas) {
        List<AddressPlanItem> items = new ArrayList<>();
        for (AddressPlanItemSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            items.add(AddressPlanItem.builder()
                    .id(schema.getId())
                    .zoneId(schema.getZoneId())
                    .subnet(schema.getSubnet())
                    .gateway(schema.getGateway())
                    .dnsServers(safeList(schema.getDnsServers()))
                    .exampleHostAddress(schema.getExampleHostAddress())
                    .hostAddressHints(safeList(schema.getHostAddressHints()))
                    .traceRefs(intentTraceRefs(schema.getTraceIntentNodeIds()))
                    .build());
        }
        return items;
    }

    private List<VlanPlanItem> mapVlanPlan(List<VlanPlanItemSchema> schemas) {
        List<VlanPlanItem> items = new ArrayList<>();
        for (VlanPlanItemSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            List<PortRef> accessPorts = new ArrayList<>();
            for (PortRefSchema pr : safeList(schema.getAccessPorts())) {
                if (pr != null) {
                    accessPorts.add(PortRef.builder()
                            .deviceId(pr.getDeviceId())
                            .interfaceName(pr.getInterfaceName())
                            .description(pr.getDescription())
                            .build());
                }
            }
            List<PortRef> trunkPorts = new ArrayList<>();
            for (PortRefSchema pr : safeList(schema.getTrunkPorts())) {
                if (pr != null) {
                    trunkPorts.add(PortRef.builder()
                            .deviceId(pr.getDeviceId())
                            .interfaceName(pr.getInterfaceName())
                            .description(pr.getDescription())
                            .build());
                }
            }
            items.add(VlanPlanItem.builder()
                    .id(schema.getId())
                    .vlanId(schema.getVlanId())
                    .name(schema.getName())
                    .zoneId(schema.getZoneId())
                    .accessPorts(accessPorts)
                    .trunkPorts(trunkPorts)
                    .traceRefs(intentTraceRefs(schema.getTraceIntentNodeIds()))
                    .build());
        }
        return items;
    }

    private RoutingPlan mapRoutingPlan(RoutingPlanSchema schema, PlanningResponseSchema fullSchema) {
        if (schema == null) {
            return null;
        }
        List<String> routingTraceIds = firstNonEmpty(
                schema.getTraceIntentNodeIds(),
                collectIntentNodeIds(fullSchema));
        List<RoutingRouter> routers = new ArrayList<>();
        for (RoutingRouterSchema rr : safeList(schema.getRouters())) {
            if (rr == null) {
                continue;
            }
            routers.add(RoutingRouter.builder()
                    .id(rr.getId())
                    .deviceId(rr.getDeviceId())
                    .routerId(rr.getRouterId())
                    .advertisedNetworks(safeList(rr.getAdvertisedNetworks()))
                    .traceRefs(intentTraceRefs(firstNonEmpty(rr.getTraceIntentNodeIds(), routingTraceIds)))
                    .build());
        }
        if (routers.isEmpty()) {
            TopologyNodeSchema routerNode = firstRouterNode(fullSchema);
            if (routerNode != null) {
                List<String> routerTraceIds = firstNonEmpty(routerNode.getTraceIntentNodeIds(), routingTraceIds);
                routers.add(RoutingRouter.builder()
                        .id("router-" + routerNode.getId())
                        .deviceId(routerNode.getId())
                        .advertisedNetworks(List.of())
                        .traceRefs(intentTraceRefs(routerTraceIds))
                        .build());
            }
        }
        DefaultRoute defaultRoute = null;
        if (schema.getDefaultRoute() != null) {
            DefaultRouteSchema dr = schema.getDefaultRoute();
            defaultRoute = DefaultRoute.builder()
                    .enabled(dr.getEnabled())
                    .nextHop(dr.getNextHop())
                    .outInterface(dr.getOutInterface() != null
                            ? PortRef.builder().deviceId(dr.getOutInterface()).build()
                            : null)
                    .build();
        }
        return RoutingPlan.builder()
                .id(schema.getId())
                .protocol(schema.getProtocol())
                .area(schema.getArea())
                .routers(routers)
                .defaultRoute(defaultRoute)
                .traceRefs(intentTraceRefs(routingTraceIds))
                .build();
    }

    private List<SecurityPolicyPlanItem> mapSecurityPolicies(List<SecurityPolicySchema> schemas) {
        List<SecurityPolicyPlanItem> items = new ArrayList<>();
        for (SecurityPolicySchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            EnforcementPoint ep = null;
            if (schema.getEnforcementDeviceId() != null || schema.getEnforcementInterfaceName() != null) {
                ep = EnforcementPoint.builder()
                        .deviceId(schema.getEnforcementDeviceId())
                        .interfaceName(schema.getEnforcementInterfaceName())
                        .direction(schema.getEnforcementDirection())
                        .build();
            }
            TraceRefs tr = new TraceRefs();
            tr.setIntentNodeIds(safeList(schema.getTraceIntentNodeIds()));
            tr.setIntentRelationIds(safeList(schema.getTraceIntentRelationIds()));
            items.add(SecurityPolicyPlanItem.builder()
                    .id(schema.getId())
                    .name(schema.getName())
                    .sourceZone(schema.getSourceZone())
                    .targetZone(schema.getTargetZone())
                    .action(schema.getAction())
                    .service(schema.getService())
                    .enforcementPoint(ep)
                    .basedOnIntentRelation(schema.getBasedOnIntentRelation())
                    .traceRefs(tr)
                    .build());
        }
        return items;
    }

    private NatPlan mapNatPlan(NatPlanSchema schema) {
        if (schema == null) {
            return null;
        }
        PortRef outsideInterface = null;
        if (schema.getOutsideInterface() != null) {
            PortRefSchema pr = schema.getOutsideInterface();
            outsideInterface = PortRef.builder()
                    .deviceId(pr.getDeviceId())
                    .interfaceName(pr.getInterfaceName())
                    .description(pr.getDescription())
                    .build();
        }
        return NatPlan.builder()
                .id(schema.getId())
                .enabled(schema.getEnabled())
                .insideZones(safeList(schema.getInsideZones()))
                .outsideInterface(outsideInterface)
                .description(schema.getDescription())
                .traceRefs(intentTraceRefs(schema.getTraceIntentNodeIds()))
                .build();
    }

    private List<PlanConstraint> mapPlanConstraints(List<PlanConstraintSchema> schemas) {
        List<PlanConstraint> constraints = new ArrayList<>();
        for (PlanConstraintSchema schema : safeList(schemas)) {
            if (schema == null) {
                continue;
            }
            constraints.add(PlanConstraint.builder()
                    .id(schema.getId())
                    .type(schema.getType())
                    .description(schema.getDescription())
                    .sourceIntentId(schema.getSourceIntentId())
                    .build());
        }
        return constraints;
    }

    private TraceRefs buildTraceRefs(PlanningResponseSchema schema) {
        List<String> intentNodeIds = new ArrayList<>();
        for (TopologyNodeSchema n : safeList(schema.getTopologyNodes())) {
            if (n != null) {
                intentNodeIds.addAll(safeList(n.getTraceIntentNodeIds()));
            }
        }
        for (AddressPlanItemSchema a : safeList(schema.getAddressPlan())) {
            if (a != null) {
                intentNodeIds.addAll(safeList(a.getTraceIntentNodeIds()));
            }
        }
        for (SecurityPolicySchema sp : safeList(schema.getSecurityPolicies())) {
            if (sp != null) {
                intentNodeIds.addAll(safeList(sp.getTraceIntentNodeIds()));
            }
        }
        if (schema.getRoutingPlan() != null) {
            intentNodeIds.addAll(safeList(schema.getRoutingPlan().getTraceIntentNodeIds()));
            for (RoutingRouterSchema router : safeList(schema.getRoutingPlan().getRouters())) {
                if (router != null) {
                    intentNodeIds.addAll(safeList(router.getTraceIntentNodeIds()));
                }
            }
        }
        TraceRefs refs = new TraceRefs();
        refs.setIntentNodeIds(intentNodeIds.stream().distinct().toList());
        return refs;
    }

    private TraceRefs intentTraceRefs(List<String> ids) {
        TraceRefs refs = new TraceRefs();
        if (ids != null) {
            refs.setIntentNodeIds(new ArrayList<>(ids));
        }
        return refs;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private List<String> collectIntentNodeIds(PlanningResponseSchema schema) {
        Set<String> ids = new LinkedHashSet<>();
        for (TopologyNodeSchema node : safeList(schema.getTopologyNodes())) {
            if (node != null) {
                ids.addAll(safeList(node.getTraceIntentNodeIds()));
            }
        }
        for (ZoneSchema zone : safeList(schema.getZones())) {
            if (zone != null && zone.getMappedFromIntentNode() != null && !zone.getMappedFromIntentNode().isBlank()) {
                ids.add(zone.getMappedFromIntentNode());
            }
        }
        for (AddressPlanItemSchema address : safeList(schema.getAddressPlan())) {
            if (address != null) {
                ids.addAll(safeList(address.getTraceIntentNodeIds()));
            }
        }
        for (SecurityPolicySchema policy : safeList(schema.getSecurityPolicies())) {
            if (policy != null) {
                ids.addAll(safeList(policy.getTraceIntentNodeIds()));
            }
        }
        return new ArrayList<>(ids);
    }

    private TopologyNodeSchema firstRouterNode(PlanningResponseSchema schema) {
        for (TopologyNodeSchema node : safeList(schema.getTopologyNodes())) {
            if (node == null) {
                continue;
            }
            if ("ROUTER".equalsIgnoreCase(node.getNodeType()) || "GATEWAY".equalsIgnoreCase(node.getRole())) {
                return node;
            }
        }
        return null;
    }

    private <T> List<T> firstNonEmpty(List<T> preferred, List<T> fallback) {
        return preferred != null && !preferred.isEmpty() ? preferred : safeList(fallback);
    }
}
