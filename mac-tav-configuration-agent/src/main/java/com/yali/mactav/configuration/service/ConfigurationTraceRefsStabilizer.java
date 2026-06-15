package com.yali.mactav.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.config.CommandBlock;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.plan.AddressPlanItem;
import com.yali.mactav.model.plan.NatPlan;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.NetworkZone;
import com.yali.mactav.model.plan.PlanConstraint;
import com.yali.mactav.model.plan.PortRef;
import com.yali.mactav.model.plan.RoutingPlan;
import com.yali.mactav.model.plan.RoutingRouter;
import com.yali.mactav.model.plan.SecurityPolicyPlanItem;
import com.yali.mactav.model.plan.SelectedArchitecture;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyLink;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.plan.VlanPlanItem;
import com.yali.mactav.model.workspace.TraceRefs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Completes missing ConfigSet command-block trace references from the real NetworkPlan.
 */
public class ConfigurationTraceRefsStabilizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationTraceRefsStabilizer.class);

    private final ObjectMapper objectMapper;

    public ConfigurationTraceRefsStabilizer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public ConfigSet stabilize(ConfigSet configSet, ConfigurationAgentInvokePayload payload) {
        if (configSet == null) {
            return null;
        }
        NetworkPlan networkPlan = readNetworkPlan(payload);
        TraceIndex traceIndex = TraceIndex.from(networkPlan);
        long start = System.nanoTime();
        int totalCommandBlockCount = commandBlockCount(configSet);
        for (DeviceConfig deviceConfig : safeList(configSet.getDeviceConfigs())) {
            if (deviceConfig == null) {
                continue;
            }
            for (CommandBlock block : safeList(deviceConfig.getCommandBlocks())) {
                if (block == null) {
                    continue;
                }
                stabilizeBlock(configSet, payload, networkPlan, traceIndex, deviceConfig, block, totalCommandBlockCount, start);
            }
        }
        return configSet;
    }

    private void stabilizeBlock(ConfigSet configSet,
                                ConfigurationAgentInvokePayload payload,
                                NetworkPlan networkPlan,
                                TraceIndex traceIndex,
                                DeviceConfig deviceConfig,
                                CommandBlock block,
                                int totalCommandBlockCount,
                                long startNanos) {
        TraceRefs existing = normalize(block.getTraceRefs());
        TraceRefs filtered = filterExisting(existing, traceIndex);
        if (hasPlanOrIntent(filtered)) {
            block.setTraceRefs(filtered);
            return;
        }
        TraceRefs derived = deriveTraceRefs(networkPlan, traceIndex, deviceConfig, block, totalCommandBlockCount);
        if (!hasPlanOrIntent(derived)) {
            LOGGER.warn(
                    "Configuration traceRefs missing taskId={}, traceId={}, blockId={}, deviceId={}, feature={}, commandCount={}, derivedPlanElementIdCount=0, derivedIntentRelationIdCount=0, durationMs={}",
                    configSet.getTaskId(),
                    payload == null ? null : payload.getTraceId(),
                    block.getBlockId(),
                    deviceConfig.getDeviceId(),
                    featureOf(block),
                    block.getCommands() == null ? 0 : block.getCommands().size(),
                    elapsedMillis(startNanos));
            block.setTraceRefs(existing);
            return;
        }
        mergeInto(filtered, derived);
        block.setTraceRefs(filtered);
        LOGGER.info(
                "Configuration traceRefs derived taskId={}, traceId={}, blockId={}, deviceId={}, feature={}, commandCount={}, derivedPlanElementIdCount={}, derivedIntentRelationIdCount={}, durationMs={}",
                configSet.getTaskId(),
                payload == null ? null : payload.getTraceId(),
                block.getBlockId(),
                deviceConfig.getDeviceId(),
                featureOf(block),
                block.getCommands() == null ? 0 : block.getCommands().size(),
                derived.getPlanElementIds().size(),
                derived.getIntentRelationIds().size(),
                elapsedMillis(startNanos));
    }

    private NetworkPlan readNetworkPlan(ConfigurationAgentInvokePayload payload) {
        if (payload == null || isBlank(payload.getPlanJson())) {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID,
                    "Configuration traceRefs cannot be stabilized without NetworkPlan JSON");
        }
        try {
            return objectMapper.readValue(payload.getPlanJson(), NetworkPlan.class);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.AGENT_PARSE_FAILED,
                    "Configuration traceRefs cannot parse NetworkPlan JSON");
        }
    }

    private TraceRefs deriveTraceRefs(NetworkPlan networkPlan,
                                      TraceIndex traceIndex,
                                      DeviceConfig deviceConfig,
                                      CommandBlock block,
                                      int totalCommandBlockCount) {
        TraceRefs refs = emptyRefs();
        String text = searchableText(deviceConfig, block);
        deriveFromSecurityPolicies(refs, safeList(networkPlan.getSecurityPolicyPlan()), text);
        deriveFromVlanPlan(refs, safeList(networkPlan.getVlanPlan()), deviceConfig, text);
        deriveFromInterfaceOrTrunk(refs, networkPlan, deviceConfig, text);
        deriveFromRoutingPlan(refs, networkPlan.getRoutingPlan(), deviceConfig, text);
        deriveFromDevice(refs, traceIndex, deviceConfig, text);
        if (!hasPlanOrIntent(refs)) {
            deriveFromUniqueSecurityPolicy(refs, safeList(networkPlan.getSecurityPolicyPlan()), totalCommandBlockCount);
        }
        filterInPlace(refs, traceIndex);
        return refs;
    }

    private void deriveFromSecurityPolicies(TraceRefs refs,
                                            List<SecurityPolicyPlanItem> policies,
                                            String text) {
        for (SecurityPolicyPlanItem policy : policies) {
            if (policy == null || !matchesSecurityPolicy(policy, text)) {
                continue;
            }
            addPlanElement(refs, policy.getId());
            addIntentRelation(refs, policy.getBasedOnIntentRelation());
            mergeInto(refs, policy.getTraceRefs());
        }
    }

    private boolean matchesSecurityPolicy(SecurityPolicyPlanItem policy, String text) {
        return containsToken(text, policy.getId())
                || containsToken(text, policy.getName())
                || containsToken(text, policy.getSourceZone())
                || containsToken(text, policy.getTargetZone())
                || containsToken(text, policy.getBasedOnIntentRelation())
                || (containsToken(text, policy.getAction()) && containsToken(text, policy.getService()));
    }

    private void deriveFromVlanPlan(TraceRefs refs,
                                    List<VlanPlanItem> vlanPlan,
                                    DeviceConfig deviceConfig,
                                    String text) {
        if (!looksLikeVlanBlock(text)) {
            return;
        }
        for (VlanPlanItem item : vlanPlan) {
            if (item == null || !matchesVlan(item, deviceConfig, text, vlanPlan.size())) {
                continue;
            }
            addPlanElement(refs, item.getId());
            addPlanElement(refs, item.getZoneId());
            mergeInto(refs, item.getTraceRefs());
        }
    }

    private boolean matchesVlan(VlanPlanItem item,
                                DeviceConfig deviceConfig,
                                String text,
                                int vlanPlanSize) {
        return containsToken(text, item.getId())
                || containsToken(text, item.getName())
                || containsToken(text, item.getZoneId())
                || (item.getVlanId() != null && containsToken(text, String.valueOf(item.getVlanId())))
                || portListMatches(item.getAccessPorts(), deviceConfig, text)
                || portListMatches(item.getTrunkPorts(), deviceConfig, text)
                || (vlanPlanSize == 1 && looksLikeVlanBlock(text));
    }

    private void deriveFromInterfaceOrTrunk(TraceRefs refs,
                                            NetworkPlan networkPlan,
                                            DeviceConfig deviceConfig,
                                            String text) {
        if (!looksLikeInterfaceBlock(text)) {
            return;
        }
        Topology topology = networkPlan == null ? null : networkPlan.getTopology();
        if (topology != null) {
            for (TopologyLink link : safeList(topology.getLinks())) {
                if (matchesTopologyLink(link, deviceConfig, text)) {
                    addPlanElement(refs, link.getId());
                    mergeInto(refs, link.getTraceRefs());
                }
            }
        }
        for (VlanPlanItem item : safeList(networkPlan == null ? null : networkPlan.getVlanPlan())) {
            if (portListMatches(item.getAccessPorts(), deviceConfig, text)
                    || portListMatches(item.getTrunkPorts(), deviceConfig, text)) {
                addPlanElement(refs, item.getId());
                mergeInto(refs, item.getTraceRefs());
            }
        }
    }

    private boolean matchesTopologyLink(TopologyLink link, DeviceConfig deviceConfig, String text) {
        if (link == null) {
            return false;
        }
        String deviceId = firstNonBlank(deviceConfig == null ? null : deviceConfig.getDeviceId(),
                deviceConfig == null ? null : deviceConfig.getDeviceName());
        boolean deviceMatches = containsToken(text, link.getSourceNode())
                || containsToken(text, link.getTargetNode())
                || sameToken(deviceId, link.getSourceNode())
                || sameToken(deviceId, link.getTargetNode());
        boolean interfaceMatches = containsToken(text, link.getSourceInterface())
                || containsToken(text, link.getTargetInterface());
        return containsToken(text, link.getId()) || (deviceMatches && (interfaceMatches || looksLikeTrunkBlock(text)));
    }

    private void deriveFromRoutingPlan(TraceRefs refs,
                                       RoutingPlan routingPlan,
                                       DeviceConfig deviceConfig,
                                       String text) {
        if (routingPlan == null || !looksLikeRoutingBlock(text)) {
            return;
        }
        addPlanElement(refs, routingPlan.getId());
        mergeInto(refs, routingPlan.getTraceRefs());
        for (RoutingRouter router : safeList(routingPlan.getRouters())) {
            if (matchesRoutingRouter(router, deviceConfig, text, routingPlan.getRouters().size())) {
                addPlanElement(refs, router.getId());
                mergeInto(refs, router.getTraceRefs());
            }
        }
    }

    private boolean matchesRoutingRouter(RoutingRouter router,
                                         DeviceConfig deviceConfig,
                                         String text,
                                         int routerCount) {
        if (router == null) {
            return false;
        }
        String deviceId = firstNonBlank(deviceConfig == null ? null : deviceConfig.getDeviceId(),
                deviceConfig == null ? null : deviceConfig.getDeviceName());
        return containsToken(text, router.getId())
                || containsToken(text, router.getDeviceId())
                || containsToken(text, router.getRouterId())
                || sameToken(deviceId, router.getDeviceId())
                || (routerCount == 1 && looksLikeRoutingBlock(text));
    }

    private void deriveFromDevice(TraceRefs refs,
                                  TraceIndex traceIndex,
                                  DeviceConfig deviceConfig,
                                  String text) {
        String deviceId = firstNonBlank(deviceConfig.getDeviceId(), deviceConfig.getDeviceName());
        TraceRefs deviceRefs = traceIndex.deviceTraceRefs.get(deviceId);
        if (deviceRefs == null && containsAny(text, traceIndex.deviceTraceRefs.keySet())) {
            for (Map.Entry<String, TraceRefs> entry : traceIndex.deviceTraceRefs.entrySet()) {
                if (containsToken(text, entry.getKey())) {
                    deviceRefs = entry.getValue();
                    break;
                }
            }
        }
        mergeInto(refs, deviceRefs);
    }

    private boolean portListMatches(List<PortRef> ports, DeviceConfig deviceConfig, String text) {
        for (PortRef port : safeList(ports)) {
            if (portMatches(port, deviceConfig, text)) {
                return true;
            }
        }
        return false;
    }

    private boolean portMatches(PortRef port, DeviceConfig deviceConfig, String text) {
        if (port == null) {
            return false;
        }
        String deviceId = firstNonBlank(deviceConfig == null ? null : deviceConfig.getDeviceId(),
                deviceConfig == null ? null : deviceConfig.getDeviceName());
        boolean deviceMatches = containsToken(text, port.getDeviceId()) || sameToken(deviceId, port.getDeviceId());
        boolean interfaceMatches = containsToken(text, port.getInterfaceName())
                || containsToken(text, port.getDescription());
        return deviceMatches && (interfaceMatches || looksLikeTrunkBlock(text) || looksLikeVlanBlock(text));
    }

    private boolean looksLikeVlanBlock(String text) {
        return containsToken(text, "vlan");
    }

    private boolean looksLikeInterfaceBlock(String text) {
        return containsToken(text, "interface")
                || containsToken(text, "trunk")
                || containsToken(text, "port")
                || containsToken(text, "ethernet");
    }

    private boolean looksLikeTrunkBlock(String text) {
        return containsToken(text, "trunk") || containsToken(text, "eth-trunk");
    }

    private boolean looksLikeRoutingBlock(String text) {
        return containsToken(text, "routing")
                || containsToken(text, "route")
                || containsToken(text, "static")
                || containsToken(text, "default")
                || containsToken(text, "ospf")
                || containsToken(text, "bgp");
    }

    private void deriveFromUniqueSecurityPolicy(TraceRefs refs, List<SecurityPolicyPlanItem> policies, int totalCommandBlockCount) {
        if (totalCommandBlockCount != 1) {
            return;
        }
        List<SecurityPolicyPlanItem> traceablePolicies = policies.stream()
                .filter(policy -> policy != null
                        && (!isBlank(policy.getId())
                        || !isBlank(policy.getBasedOnIntentRelation())
                        || hasPlanOrIntent(policy.getTraceRefs())))
                .toList();
        if (traceablePolicies.size() != 1) {
            return;
        }
        SecurityPolicyPlanItem policy = traceablePolicies.get(0);
        addPlanElement(refs, policy.getId());
        addIntentRelation(refs, policy.getBasedOnIntentRelation());
        mergeInto(refs, policy.getTraceRefs());
    }

    private TraceRefs filterExisting(TraceRefs existing, TraceIndex traceIndex) {
        TraceRefs filtered = emptyRefs();
        addAll(filtered.getIntentNodeIds(), validOrUnknown(existing.getIntentNodeIds(), traceIndex.intentNodeIds));
        addAll(filtered.getIntentRelationIds(), validOrUnknown(existing.getIntentRelationIds(), traceIndex.intentRelationIds));
        addAll(filtered.getPlanElementIds(), validOrUnknown(existing.getPlanElementIds(), traceIndex.planElementIds));
        addAll(filtered.getConfigBlockIds(), existing.getConfigBlockIds());
        addAll(filtered.getTestIds(), existing.getTestIds());
        addAll(filtered.getValidationItemIds(), existing.getValidationItemIds());
        addAll(filtered.getRepairActionIds(), existing.getRepairActionIds());
        return filtered;
    }

    private List<String> validOrUnknown(List<String> values, Set<String> knownValues) {
        if (knownValues.isEmpty()) {
            return safeList(values);
        }
        return safeList(values).stream().filter(knownValues::contains).toList();
    }

    private void filterInPlace(TraceRefs refs, TraceIndex traceIndex) {
        refs.setIntentNodeIds(validOrUnknown(refs.getIntentNodeIds(), traceIndex.intentNodeIds));
        refs.setIntentRelationIds(validOrUnknown(refs.getIntentRelationIds(), traceIndex.intentRelationIds));
        refs.setPlanElementIds(validOrUnknown(refs.getPlanElementIds(), traceIndex.planElementIds));
    }

    private String searchableText(DeviceConfig deviceConfig, CommandBlock block) {
        List<String> values = new ArrayList<>();
        values.add(deviceConfig == null ? null : deviceConfig.getDeviceId());
        values.add(deviceConfig == null ? null : deviceConfig.getDeviceName());
        values.add(deviceConfig == null ? null : deviceConfig.getDeviceType());
        values.add(block == null ? null : block.getBlockId());
        values.add(block == null ? null : block.getBlockType());
        values.add(block == null ? null : block.getTitle());
        values.add(block == null ? null : block.getExplanation());
        if (block != null) {
            values.addAll(safeList(block.getCommands()));
        }
        return String.join(" ", values.stream().filter(value -> !isBlank(value)).toList())
                .toLowerCase(Locale.ROOT);
    }

    private String featureOf(CommandBlock block) {
        return firstNonBlank(block.getBlockType(), block.getTitle());
    }

    private int commandBlockCount(ConfigSet configSet) {
        if (configSet == null) {
            return 0;
        }
        int count = 0;
        for (DeviceConfig deviceConfig : safeList(configSet.getDeviceConfigs())) {
            count += safeList(deviceConfig.getCommandBlocks()).size();
        }
        return count;
    }

    private TraceRefs normalize(TraceRefs refs) {
        if (refs == null) {
            return emptyRefs();
        }
        if (refs.getIntentNodeIds() == null) {
            refs.setIntentNodeIds(new ArrayList<>());
        }
        if (refs.getIntentRelationIds() == null) {
            refs.setIntentRelationIds(new ArrayList<>());
        }
        if (refs.getPlanElementIds() == null) {
            refs.setPlanElementIds(new ArrayList<>());
        }
        if (refs.getConfigBlockIds() == null) {
            refs.setConfigBlockIds(new ArrayList<>());
        }
        if (refs.getTestIds() == null) {
            refs.setTestIds(new ArrayList<>());
        }
        if (refs.getValidationItemIds() == null) {
            refs.setValidationItemIds(new ArrayList<>());
        }
        if (refs.getRepairActionIds() == null) {
            refs.setRepairActionIds(new ArrayList<>());
        }
        return refs;
    }

    private TraceRefs emptyRefs() {
        return TraceRefs.builder().build();
    }

    private void mergeInto(TraceRefs target, TraceRefs source) {
        if (target == null || source == null) {
            return;
        }
        addAll(target.getIntentNodeIds(), source.getIntentNodeIds());
        addAll(target.getIntentRelationIds(), source.getIntentRelationIds());
        addAll(target.getPlanElementIds(), source.getPlanElementIds());
        addAll(target.getConfigBlockIds(), source.getConfigBlockIds());
        addAll(target.getTestIds(), source.getTestIds());
        addAll(target.getValidationItemIds(), source.getValidationItemIds());
        addAll(target.getRepairActionIds(), source.getRepairActionIds());
    }

    private void addPlanElement(TraceRefs refs, String value) {
        add(refs.getPlanElementIds(), value);
    }

    private void addIntentRelation(TraceRefs refs, String value) {
        add(refs.getIntentRelationIds(), value);
    }

    private void addAll(List<String> target, Collection<String> values) {
        for (String value : safeList(values)) {
            add(target, value);
        }
    }

    private void add(List<String> target, String value) {
        if (!isBlank(value) && !target.contains(value)) {
            target.add(value);
        }
    }

    private boolean hasPlanOrIntent(TraceRefs refs) {
        return refs != null && (!safeList(refs.getPlanElementIds()).isEmpty()
                || !safeList(refs.getIntentRelationIds()).isEmpty());
    }

    private boolean containsAny(String text, Collection<String> candidates) {
        for (String candidate : safeList(candidates)) {
            if (containsToken(text, candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsToken(String text, String token) {
        return !isBlank(text) && !isBlank(token) && text.contains(token.toLowerCase(Locale.ROOT));
    }

    private boolean sameToken(String left, String right) {
        return !isBlank(left) && !isBlank(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private <T> List<T> safeList(Collection<T> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static final class TraceIndex {

        private final Set<String> planElementIds = new LinkedHashSet<>();

        private final Set<String> intentNodeIds = new LinkedHashSet<>();

        private final Set<String> intentRelationIds = new LinkedHashSet<>();

        private final Map<String, TraceRefs> deviceTraceRefs = new LinkedHashMap<>();

        static TraceIndex from(NetworkPlan plan) {
            TraceIndex index = new TraceIndex();
            index.indexPlan(plan);
            return index;
        }

        private void indexPlan(NetworkPlan plan) {
            if (plan == null) {
                return;
            }
            addPlanElement(plan.getPlanId());
            indexTraceRefs(plan.getTraceRefs());
            indexSelectedArchitecture(plan.getSelectedArchitecture());
            indexTopology(plan.getTopology());
            safeList(plan.getZones()).forEach(this::indexZone);
            safeList(plan.getAddressPlan()).forEach(this::indexAddress);
            safeList(plan.getVlanPlan()).forEach(this::indexVlan);
            indexRouting(plan.getRoutingPlan());
            safeList(plan.getSecurityPolicyPlan()).forEach(this::indexSecurityPolicy);
            indexNat(plan.getNatPlan());
            safeList(plan.getPlanConstraints()).forEach(this::indexConstraint);
        }

        private void indexSelectedArchitecture(SelectedArchitecture architecture) {
            if (architecture != null) {
                addPlanElement(architecture.getId());
            }
        }

        private void indexTopology(Topology topology) {
            if (topology == null) {
                return;
            }
            safeList(topology.getNodes()).forEach(node -> {
                addPlanElement(node.getId());
                indexTraceRefs(node.getTraceRefs());
                TraceRefs nodeRefs = traceRefsWithOwnPlanElement(node.getId(), node.getTraceRefs());
                if (!isBlank(node.getId())) {
                    deviceTraceRefs.put(node.getId(), nodeRefs);
                }
                if (!isBlank(node.getName())) {
                    deviceTraceRefs.put(node.getName(), nodeRefs);
                }
            });
            safeList(topology.getLinks()).forEach(link -> {
                addPlanElement(link.getId());
                indexTraceRefs(link.getTraceRefs());
            });
        }

        private void indexZone(NetworkZone zone) {
            if (zone != null) {
                addPlanElement(zone.getId());
                addIntentNode(zone.getMappedFromIntentNode());
            }
        }

        private void indexAddress(AddressPlanItem item) {
            if (item != null) {
                addPlanElement(item.getId());
                indexTraceRefs(item.getTraceRefs());
            }
        }

        private void indexVlan(VlanPlanItem item) {
            if (item != null) {
                addPlanElement(item.getId());
                indexTraceRefs(item.getTraceRefs());
            }
        }

        private void indexRouting(RoutingPlan routingPlan) {
            if (routingPlan == null) {
                return;
            }
            addPlanElement(routingPlan.getId());
            indexTraceRefs(routingPlan.getTraceRefs());
            safeList(routingPlan.getRouters()).forEach(router -> {
                addPlanElement(router.getId());
                indexTraceRefs(router.getTraceRefs());
            });
        }

        private void indexSecurityPolicy(SecurityPolicyPlanItem policy) {
            if (policy == null) {
                return;
            }
            addPlanElement(policy.getId());
            addIntentRelation(policy.getBasedOnIntentRelation());
            indexTraceRefs(policy.getTraceRefs());
        }

        private void indexNat(NatPlan natPlan) {
            if (natPlan != null) {
                addPlanElement(natPlan.getId());
                indexTraceRefs(natPlan.getTraceRefs());
            }
        }

        private void indexConstraint(PlanConstraint constraint) {
            if (constraint != null) {
                addPlanElement(constraint.getId());
                addIntentNode(constraint.getSourceIntentId());
            }
        }

        private void indexTraceRefs(TraceRefs traceRefs) {
            if (traceRefs == null) {
                return;
            }
            intentNodeIds.addAll(safeList(traceRefs.getIntentNodeIds()));
            intentRelationIds.addAll(safeList(traceRefs.getIntentRelationIds()));
            planElementIds.addAll(safeList(traceRefs.getPlanElementIds()));
        }

        private TraceRefs traceRefsWithOwnPlanElement(String planElementId, TraceRefs source) {
            TraceRefs refs = TraceRefs.builder().build();
            addAll(refs.getIntentNodeIds(), source == null ? null : source.getIntentNodeIds());
            addAll(refs.getIntentRelationIds(), source == null ? null : source.getIntentRelationIds());
            addAll(refs.getPlanElementIds(), source == null ? null : source.getPlanElementIds());
            addAll(refs.getConfigBlockIds(), source == null ? null : source.getConfigBlockIds());
            addAll(refs.getTestIds(), source == null ? null : source.getTestIds());
            addAll(refs.getValidationItemIds(), source == null ? null : source.getValidationItemIds());
            addAll(refs.getRepairActionIds(), source == null ? null : source.getRepairActionIds());
            if (!isBlank(planElementId) && !refs.getPlanElementIds().contains(planElementId)) {
                refs.getPlanElementIds().add(planElementId);
            }
            return refs;
        }

        private void addAll(List<String> target, Collection<String> values) {
            for (String value : safeList(values)) {
                if (!isBlank(value) && !target.contains(value)) {
                    target.add(value);
                }
            }
        }

        private void addPlanElement(String value) {
            if (!isBlank(value)) {
                planElementIds.add(value);
            }
        }

        private void addIntentNode(String value) {
            if (!isBlank(value)) {
                intentNodeIds.add(value);
            }
        }

        private void addIntentRelation(String value) {
            if (!isBlank(value)) {
                intentRelationIds.add(value);
            }
        }

        private static <T> List<T> safeList(Collection<T> values) {
            return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
        }

        private static boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}
