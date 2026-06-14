package com.yali.mactav.planning.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.planning.request.PlanningAgentRequest;
import com.yali.mactav.planning.schema.PlanningResponseSchema;
import com.yali.mactav.planning.service.PlanningService;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin real PlanningAgent wrapper for Spring AI Alibaba ReactAgent.
 *
 * <p>The wrapper owns model invocation and the PlanningAgent-local tool list, then
 * delegates ResponseSchema -> Parser -> DTO -> Validator to PlanningService. It
 * must not return raw model text, write NetworkWorkspace, advance task state, or
 * depend on Web/Orchestrator/Model Core.</p>
 */
public class PlanningAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanningAgent.class);

    public static final String AGENT_NAME = "PlanningAgent";

    public static final String AGENT_DESCRIPTION = "MAC-TAV network planning and topology design agent";

    private final ReactAgent reactAgent;

    private final ObjectMapper objectMapper;

    private final PlanningService planningService;

    public PlanningAgent(ReactAgent reactAgent,
                         ObjectMapper objectMapper,
                         PlanningService planningService) {
        this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.planningService = Objects.requireNonNull(planningService, "planningService must not be null");
    }

    public NetworkPlan run(PlanningAgentRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "PlanningAgentRequest must not be null");
        }
        String input = serializeRequest(request);
        LOGGER.info(
                "PlanningAgent model call start taskId={}, traceId={}, serializedRequestLength={}",
                request.getTaskId(),
                request.getTraceId(),
                input.length());
        long modelStart = System.nanoTime();
        try {
            PlanningResponseSchema schema = AgentUtils.callSchema(reactAgent, input, PlanningResponseSchema.class);
            long modelDurationMs = elapsedMillis(modelStart);
            LOGGER.info(
                    "PlanningAgent model call completed taskId={}, traceId={}, durationMs={}",
                    request.getTaskId(),
                    request.getTraceId(),
                    modelDurationMs);
            long parseStart = System.nanoTime();
            NetworkPlan plan = planningService.parse(schema, request);
            long parseValidateDurationMs = elapsedMillis(parseStart);
            LOGGER.info(
                    "PlanningAgent parse/validator completed taskId={}, traceId={}, durationMs={}, nodeCount={}, linkCount={}, zoneCount={}, policyCount={}",
                    request.getTaskId(),
                    request.getTraceId(),
                    parseValidateDurationMs,
                    topologyNodeCount(plan),
                    topologyLinkCount(plan),
                    plan == null || plan.getZones() == null ? 0 : plan.getZones().size(),
                    plan == null || plan.getSecurityPolicyPlan() == null ? 0 : plan.getSecurityPolicyPlan().size());
            return plan;
        }
        catch (RuntimeException ex) {
            LOGGER.warn(
                    "PlanningAgent run failed taskId={}, traceId={}, errorClass={}, message={}",
                    request.getTaskId(),
                    request.getTraceId(),
                    ex.getClass().getSimpleName(),
                    summarize(ex.getMessage()));
            throw ex;
        }
    }

    private String serializeRequest(PlanningAgentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException ex) {
            throw AgentUtils.wrapException(
                    ErrorCode.AGENT_PARSE_FAILED,
                    "Failed to serialize PlanningAgent request",
                    ex
            );
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private int topologyNodeCount(NetworkPlan plan) {
        return plan == null || plan.getTopology() == null || plan.getTopology().getNodes() == null
                ? 0
                : plan.getTopology().getNodes().size();
    }

    private int topologyLinkCount(NetworkPlan plan) {
        return plan == null || plan.getTopology() == null || plan.getTopology().getLinks() == null
                ? 0
                : plan.getTopology().getLinks().size();
    }

    private String summarize(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240) + "...";
    }
}
