package com.yali.mactav.intent.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.service.IntentService;
import com.yali.mactav.intent.service.IntentSchemaStabilizer;
import com.yali.mactav.intent.tool.IntentExtractTool;
import com.yali.mactav.intent.tool.IntentExtractTool.IntentExtractionHints;
import com.yali.mactav.model.intent.NetworkIntent;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin real IntentAgent wrapper for Spring AI Alibaba ReactAgent.
 *
 * <p>The wrapper owns model invocation and the IntentAgent-local tool list, then
 * delegates ResponseSchema -> Parser -> DTO -> Validator to IntentService. It
 * must not return raw model text, write NetworkWorkspace, advance task state, or
 * depend on Web/Orchestrator/Model Core.</p>
 */
public class IntentAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentAgent.class);

    public static final String AGENT_NAME = "IntentAgent";

    public static final String AGENT_DESCRIPTION = "MAC-TAV business intent understanding agent";

    private final ReactAgent reactAgent;

    private final ObjectMapper objectMapper;

    private final IntentService intentService;

    private final IntentExtractTool intentExtractTool;

    private final IntentSchemaStabilizer schemaStabilizer;

    public IntentAgent(ReactAgent reactAgent,
                       ObjectMapper objectMapper,
                       IntentService intentService,
                       IntentExtractTool intentExtractTool,
                       IntentSchemaStabilizer schemaStabilizer) {
        this.reactAgent = Objects.requireNonNull(reactAgent, "reactAgent must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.intentService = Objects.requireNonNull(intentService, "intentService must not be null");
        this.intentExtractTool = Objects.requireNonNull(intentExtractTool, "intentExtractTool must not be null");
        this.schemaStabilizer = Objects.requireNonNull(schemaStabilizer, "schemaStabilizer must not be null");
    }

    public NetworkIntent run(IntentAgentRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IntentAgentRequest must not be null");
        }
        String input = serializeRequest(request);
        IntentExtractionHints hints = intentExtractTool.extractIntentHints(request.getRawText());
        IntentResponseSchema schema = null;
        boolean modelParseFailed = false;
        long modelStart = System.nanoTime();
        try {
            schema = AgentUtils.callSchema(reactAgent, input, IntentResponseSchema.class);
        }
        catch (BusinessException ex) {
            if (!ErrorCode.AGENT_SCHEMA_INVALID.getErrorCode().equals(ex.getErrorCode())) {
                throw ex;
            }
            modelParseFailed = true;
            LOGGER.warn(
                    "IntentAgent schema parse failed taskId={}, traceId={}, errorCode={}, durationMs={}, message={}",
                    request.getTaskId(),
                    request.getTraceId(),
                    ex.getErrorCode(),
                    elapsedMillis(modelStart),
                    summarize(ex.getMessage()));
        }
        IntentResponseSchema stabilizedSchema = schemaStabilizer.stabilize(request.getRawText(), schema, hints);
        LOGGER.info(
                "IntentAgent schema stabilized taskId={}, traceId={}, schemaSource={}, modelParseFailed={}, nodeCount={}, relationCount={}, durationMs={}",
                request.getTaskId(),
                request.getTraceId(),
                schema == null ? "tool-hints" : "model-plus-tool-hints",
                modelParseFailed,
                stabilizedSchema.getNodes() == null ? 0 : stabilizedSchema.getNodes().size(),
                stabilizedSchema.getRelations() == null ? 0 : stabilizedSchema.getRelations().size(),
                elapsedMillis(modelStart));
        return intentService.parse(stabilizedSchema, request);
    }

    private String serializeRequest(IntentAgentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        }
        catch (JsonProcessingException ex) {
            throw AgentUtils.wrapException(
                    ErrorCode.AGENT_PARSE_FAILED,
                    "Failed to serialize IntentAgent request",
                    ex
            );
        }
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String summarize(String message) {
        if (message == null) {
            return "";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "...";
    }
}
