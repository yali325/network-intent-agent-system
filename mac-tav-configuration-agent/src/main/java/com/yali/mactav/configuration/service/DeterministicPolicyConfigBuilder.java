package com.yali.mactav.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.config.CommandBlock;
import com.yali.mactav.model.config.ConfigSet;
import com.yali.mactav.model.config.ConfigWarning;
import com.yali.mactav.model.config.DeviceConfig;
import com.yali.mactav.model.plan.AddressPlanItem;
import com.yali.mactav.model.plan.EnforcementPoint;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.model.plan.SecurityPolicyPlanItem;
import com.yali.mactav.model.plan.Topology;
import com.yali.mactav.model.plan.TopologyNode;
import com.yali.mactav.model.workspace.TraceRefs;
import com.yali.mactav.model.config.ConfigurationAgentInvokePayload;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures core security policies from NetworkPlan are represented by deterministic command blocks.
 */
public class DeterministicPolicyConfigBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeterministicPolicyConfigBuilder.class);

    private final ObjectMapper objectMapper;

    public DeterministicPolicyConfigBuilder(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public ConfigSet stabilize(ConfigSet configSet, ConfigurationAgentInvokePayload payload) {
        if (configSet == null) {
            return null;
        }
        NetworkPlan networkPlan = readNetworkPlan(payload);
        if (networkPlan == null || safeList(networkPlan.getSecurityPolicyPlan()).isEmpty()) {
            return configSet;
        }
        long start = System.nanoTime();
        int added = 0;
        int corrected = 0;
        int policyIndex = 1;
        for (SecurityPolicyPlanItem policy : safeList(networkPlan.getSecurityPolicyPlan())) {
            if (!isAccessPolicy(policy)) {
                policyIndex++;
                continue;
            }
            AddressPair addressPair = resolveAddressPair(networkPlan, policy);
            DeviceConfig targetDevice = resolveDevice(configSet, networkPlan, policy);
            CommandBlock existing = findPolicyBlock(configSet, policy);
            if (existing != null && commandMatchesPolicy(existing, policy)) {
                policyIndex++;
                continue;
            }
            CommandBlock deterministicBlock = buildPolicyBlock(policy, addressPair, policyIndex);
            if (existing == null) {
                targetDevice.getCommandBlocks().add(deterministicBlock);
                added++;
            }
            else {
                applyBlock(existing, deterministicBlock);
                corrected++;
            }
            if (policy.getEnforcementPoint() == null || isBlank(policy.getEnforcementPoint().getInterfaceName())) {
                addWarning(configSet, deterministicBlock.getBlockId(),
                        "Policy command block generated without a reliable interface binding; execution adapters treat it as configuration evidence only.");
            }
            policyIndex++;
        }
        LOGGER.info(
                "Deterministic policy config stabilized taskId={}, traceId={}, addedPolicyBlocks={}, correctedPolicyBlocks={}, deviceCount={}, commandBlockCount={}, durationMs={}",
                configSet.getTaskId(),
                payload == null ? null : payload.getTraceId(),
                added,
                corrected,
                safeList(configSet.getDeviceConfigs()).size(),
                commandBlockCount(configSet),
                elapsedMillis(start));
        return configSet;
    }

    private NetworkPlan readNetworkPlan(ConfigurationAgentInvokePayload payload) {
        if (payload == null || isBlank(payload.getPlanJson())) {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID,
                    "Deterministic policy config requires NetworkPlan JSON");
        }
        try {
            return objectMapper.readValue(payload.getPlanJson(), NetworkPlan.class);
        }
        catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.AGENT_PARSE_FAILED,
                    "Deterministic policy config cannot parse NetworkPlan JSON", ex);
        }
    }

    private boolean isAccessPolicy(SecurityPolicyPlanItem policy) {
        if (policy == null) {
            return false;
        }
        String action = normalize(policy.getAction());
        return action.contains("DENY")
                || action.contains("DROP")
                || action.contains("REJECT")
                || action.contains("BLOCK")
                || action.contains("ALLOW")
                || action.contains("PERMIT")
                || action.contains("ACCEPT");
    }

    private AddressPair resolveAddressPair(NetworkPlan networkPlan, SecurityPolicyPlanItem policy) {
        AddressPlanItem source = findAddress(networkPlan, policy.getSourceZone())
                .orElseThrow(() -> missingAddress(policy, policy.getSourceZone(), "sourceZone"));
        AddressPlanItem target = findAddress(networkPlan, policy.getTargetZone())
                .orElseThrow(() -> missingAddress(policy, policy.getTargetZone(), "targetZone"));
        return new AddressPair(cidrToAclAddress(source.getSubnet(), policy, "sourceZone"),
                cidrToAclAddress(target.getSubnet(), policy, "targetZone"));
    }

    private Optional<AddressPlanItem> findAddress(NetworkPlan networkPlan, String zoneId) {
        if (networkPlan == null || isBlank(zoneId)) {
            return Optional.empty();
        }
        return safeList(networkPlan.getAddressPlan()).stream()
                .filter(item -> item != null && zoneId.equalsIgnoreCase(item.getZoneId()))
                .findFirst();
    }

    private BusinessException missingAddress(SecurityPolicyPlanItem policy, String zone, String side) {
        return new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID,
                "Cannot generate deterministic policy config: missing " + side
                        + " subnet for policyId=" + valueOrFallback(policy == null ? null : policy.getId(), "unknown")
                        + ", zone=" + valueOrFallback(zone, "unknown"));
    }

    private String cidrToAclAddress(String cidr, SecurityPolicyPlanItem policy, String side) {
        if (isBlank(cidr) || !cidr.contains("/")) {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID,
                    "Cannot generate deterministic policy config: invalid " + side
                            + " subnet for policyId=" + valueOrFallback(policy.getId(), "unknown"));
        }
        String[] parts = cidr.trim().split("/");
        if (parts.length != 2) {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID,
                    "Cannot generate deterministic policy config: malformed CIDR for policyId="
                            + valueOrFallback(policy.getId(), "unknown"));
        }
        int prefix;
        try {
            prefix = Integer.parseInt(parts[1]);
        }
        catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID,
                    "Cannot generate deterministic policy config: invalid CIDR prefix for policyId="
                            + valueOrFallback(policy.getId(), "unknown"), ex);
        }
        if (prefix < 0 || prefix > 32) {
            throw new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID,
                    "Cannot generate deterministic policy config: CIDR prefix out of range for policyId="
                            + valueOrFallback(policy.getId(), "unknown"));
        }
        long mask = prefix == 0 ? 0 : (0xffffffffL << (32 - prefix)) & 0xffffffffL;
        long wildcard = (~mask) & 0xffffffffL;
        return parts[0].trim() + " " + toIpv4(wildcard);
    }

    private String toIpv4(long value) {
        return ((value >> 24) & 0xff) + "."
                + ((value >> 16) & 0xff) + "."
                + ((value >> 8) & 0xff) + "."
                + (value & 0xff);
    }

    private DeviceConfig resolveDevice(ConfigSet configSet, NetworkPlan networkPlan, SecurityPolicyPlanItem policy) {
        String preferredDeviceId = policy.getEnforcementPoint() == null ? null : policy.getEnforcementPoint().getDeviceId();
        if (!isBlank(preferredDeviceId)) {
            Optional<DeviceConfig> existing = findDevice(configSet, preferredDeviceId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (!safeList(configSet.getDeviceConfigs()).isEmpty()) {
            return configSet.getDeviceConfigs().get(0);
        }
        TopologyNode node = firstNetworkDevice(networkPlan)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_OUTPUT_INVALID,
                        "Cannot generate deterministic policy config: no deviceConfig or topology device is available"));
        DeviceConfig deviceConfig = DeviceConfig.builder()
                .deviceId(node.getId())
                .deviceName(valueOrFallback(node.getName(), node.getId()))
                .deviceType(valueOrFallback(node.getDeviceType(), valueOrFallback(node.getNodeType(), "NETWORK_DEVICE")))
                .vendor(valueOrFallback(node.getVendor(), "Huawei"))
                .commandBlocks(new ArrayList<>())
                .traceRefs(node.getTraceRefs())
                .build();
        configSet.getDeviceConfigs().add(deviceConfig);
        return deviceConfig;
    }

    private Optional<DeviceConfig> findDevice(ConfigSet configSet, String deviceId) {
        return safeList(configSet.getDeviceConfigs()).stream()
                .filter(device -> device != null
                        && (same(device.getDeviceId(), deviceId) || same(device.getDeviceName(), deviceId)))
                .findFirst();
    }

    private Optional<TopologyNode> firstNetworkDevice(NetworkPlan networkPlan) {
        Topology topology = networkPlan == null ? null : networkPlan.getTopology();
        return safeList(topology == null ? null : topology.getNodes()).stream()
                .filter(node -> node != null && !isBlank(node.getId()))
                .filter(node -> {
                    String type = normalize(firstNonBlank(node.getDeviceType(), node.getNodeType()));
                    return type.contains("SWITCH") || type.contains("ROUTER") || type.contains("GATEWAY");
                })
                .findFirst();
    }

    private CommandBlock findPolicyBlock(ConfigSet configSet, SecurityPolicyPlanItem policy) {
        for (DeviceConfig deviceConfig : safeList(configSet.getDeviceConfigs())) {
            for (CommandBlock block : safeList(deviceConfig == null ? null : deviceConfig.getCommandBlocks())) {
                if (blockReferencesPolicy(block, policy)) {
                    return block;
                }
            }
        }
        return null;
    }

    private boolean blockReferencesPolicy(CommandBlock block, SecurityPolicyPlanItem policy) {
        if (block == null || policy == null) {
            return false;
        }
        TraceRefs refs = block.getTraceRefs();
        return contains(refs == null ? null : refs.getPlanElementIds(), policy.getId())
                || contains(refs == null ? null : refs.getIntentRelationIds(), policy.getBasedOnIntentRelation())
                || textContains(block, policy.getId())
                || textContains(block, policy.getBasedOnIntentRelation());
    }

    private boolean commandMatchesPolicy(CommandBlock block, SecurityPolicyPlanItem policy) {
        String commandText = commandText(block);
        String expected = expectedAclAction(policy);
        String opposite = "deny".equals(expected) ? "permit" : "deny";
        return commandText.contains("rule " + expected)
                && !commandText.contains("rule " + opposite + " ip");
    }

    private CommandBlock buildPolicyBlock(SecurityPolicyPlanItem policy, AddressPair addressPair, int policyIndex) {
        String blockId = "cb-policy-" + safeId(policy.getId(), String.valueOf(policyIndex));
        String aclNumber = String.valueOf(3000 + policyIndex);
        String aclAction = expectedAclAction(policy);
        List<String> commands = new ArrayList<>();
        commands.add("acl number " + aclNumber);
        commands.add("rule " + aclAction + " ip source " + addressPair.sourceAcl()
                + " destination " + addressPair.targetAcl());
        EnforcementPoint enforcementPoint = policy.getEnforcementPoint();
        if (enforcementPoint != null && !isBlank(enforcementPoint.getInterfaceName())) {
            commands.add("interface " + enforcementPoint.getInterfaceName());
            commands.add("traffic-filter " + valueOrFallback(enforcementPoint.getDirection(), "inbound")
                    + " acl " + aclNumber);
        }
        commands.add("return");
        return CommandBlock.builder()
                .blockId(blockId)
                .blockType("ACL_POLICY")
                .title("Deterministic policy ACL for " + valueOrFallback(policy.getName(), policy.getId()))
                .commands(commands)
                .explanation("Generated deterministic ACL commands from NetworkPlan securityPolicyPlan.")
                .rollbackCommands(List.of("undo acl number " + aclNumber))
                .rollbackStrategy("Remove generated ACL policy block if rollback is required.")
                .traceRefs(traceRefsFor(policy, blockId))
                .riskLevel("MEDIUM")
                .isIdempotent(true)
                .build();
    }

    private void applyBlock(CommandBlock target, CommandBlock source) {
        target.setBlockType(source.getBlockType());
        target.setTitle(source.getTitle());
        target.setCommands(source.getCommands());
        target.setExplanation(source.getExplanation());
        target.setRollbackCommands(source.getRollbackCommands());
        target.setRollbackStrategy(source.getRollbackStrategy());
        target.setTraceRefs(source.getTraceRefs());
        target.setRiskLevel(source.getRiskLevel());
        target.setIsIdempotent(source.getIsIdempotent());
    }

    private TraceRefs traceRefsFor(SecurityPolicyPlanItem policy, String blockId) {
        TraceRefs refs = TraceRefs.builder().build();
        mergeInto(refs, policy.getTraceRefs());
        add(refs.getPlanElementIds(), policy.getId());
        add(refs.getIntentRelationIds(), policy.getBasedOnIntentRelation());
        add(refs.getConfigBlockIds(), blockId);
        return refs;
    }

    private void mergeInto(TraceRefs target, TraceRefs source) {
        if (target == null || source == null) {
            return;
        }
        addAll(target.getIntentNodeIds(), source.getIntentNodeIds());
        addAll(target.getIntentRelationIds(), source.getIntentRelationIds());
        addAll(target.getPlanElementIds(), source.getPlanElementIds());
        addAll(target.getConfigBlockIds(), source.getConfigBlockIds());
    }

    private void addWarning(ConfigSet configSet, String blockId, String message) {
        configSet.getWarnings().add(ConfigWarning.builder()
                .level("LOW")
                .message(message)
                .relatedBlockId(blockId)
                .build());
    }

    private String expectedAclAction(SecurityPolicyPlanItem policy) {
        String action = normalize(policy == null ? null : policy.getAction());
        if (action.contains("ALLOW") || action.contains("PERMIT") || action.contains("ACCEPT")) {
            return "permit";
        }
        return "deny";
    }

    private String commandText(CommandBlock block) {
        return String.join("\n", safeList(block == null ? null : block.getCommands())).toLowerCase(Locale.ROOT);
    }

    private boolean textContains(CommandBlock block, String token) {
        if (isBlank(token)) {
            return false;
        }
        String text = String.join(" ",
                valueOrFallback(block.getBlockId(), ""),
                valueOrFallback(block.getBlockType(), ""),
                valueOrFallback(block.getTitle(), ""),
                valueOrFallback(block.getExplanation(), ""),
                String.join(" ", safeList(block.getCommands())));
        return text.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
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

    private boolean contains(List<String> values, String expected) {
        if (isBlank(expected)) {
            return false;
        }
        return safeList(values).stream().anyMatch(value -> same(value, expected));
    }

    private void addAll(List<String> target, List<String> values) {
        for (String value : safeList(values)) {
            add(target, value);
        }
    }

    private void add(List<String> target, String value) {
        if (target != null && !isBlank(value) && !target.contains(value)) {
            target.add(value);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String safeId(String value, String fallback) {
        return valueOrFallback(value, fallback).replaceAll("[^A-Za-z0-9_.-]", "-");
    }

    private String firstNonBlank(String first, String second) {
        return !isBlank(first) ? first : second;
    }

    private String valueOrFallback(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean same(String left, String right) {
        return !isBlank(left) && !isBlank(right) && left.equalsIgnoreCase(right);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private record AddressPair(String sourceAcl, String targetAcl) {
    }
}
