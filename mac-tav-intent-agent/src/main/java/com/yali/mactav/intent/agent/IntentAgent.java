package com.yali.mactav.intent.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.agent.core.agent.AgentUtils;
import com.yali.mactav.agent.core.agent.SchemaAgentInvoker;
import com.yali.mactav.agent.core.hook.AgentLogHook;
import com.yali.mactav.agent.core.hook.ErrorHandlingHook;
import com.yali.mactav.agent.core.hook.PlanHook;
import com.yali.mactav.agent.core.hook.TraceHook;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.intent.config.IntentAgentProperties;
import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.service.IntentService;
import com.yali.mactav.intent.tool.IntentExtractTool;
import com.yali.mactav.model.intent.NetworkIntent;
import java.util.Objects;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Thin real IntentAgent wrapper for Spring AI Alibaba ReactAgent.
 *
 * <p>The wrapper owns model invocation and the IntentAgent-local tool list, then
 * delegates ResponseSchema -> Parser -> DTO -> Validator to IntentService. It
 * must not return raw model text, write NetworkWorkspace, advance task state, or
 * depend on Web/Orchestrator/Model Core.</p>
 */
public class IntentAgent {

    public static final String AGENT_NAME = "IntentAgent";

    public static final String AGENT_DESCRIPTION = "MAC-TAV business intent understanding agent";

    private final ReactAgent reactAgent;

    private final SchemaAgentInvoker schemaAgentInvoker;

    private final ObjectMapper objectMapper;

    private final IntentService intentService;

    public IntentAgent(ChatModel chatModel,
                       IntentExtractTool intentExtractTool,
                       ObjectMapper objectMapper,
                       IntentService intentService,
                       IntentAgentProperties properties) {
        this(
                buildReactAgent(chatModel, intentExtractTool, properties),
                null,
                objectMapper,
                intentService
        );
    }

    public IntentAgent(ReactAgent reactAgent,
                       ObjectMapper objectMapper,
                       IntentService intentService) {
        this(reactAgent, null, objectMapper, intentService);
    }

    public IntentAgent(SchemaAgentInvoker schemaAgentInvoker,
                       ObjectMapper objectMapper,
                       IntentService intentService) {
        this(null, schemaAgentInvoker, objectMapper, intentService);
    }

    private IntentAgent(ReactAgent reactAgent,
                        SchemaAgentInvoker schemaAgentInvoker,
                        ObjectMapper objectMapper,
                        IntentService intentService) {
        this.reactAgent = reactAgent;
        this.schemaAgentInvoker = schemaAgentInvoker;
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.intentService = Objects.requireNonNull(intentService, "intentService must not be null");
        if (reactAgent == null && schemaAgentInvoker == null) {
            throw new BusinessException(ErrorCode.AGENT_EXECUTION_FAILED, "IntentAgent requires a model or schema invoker");
        }
    }

    public NetworkIntent run(IntentAgentRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "IntentAgentRequest must not be null");
        }
        String input = serializeRequest(request);
        IntentResponseSchema schema = schemaAgentInvoker == null
                ? AgentUtils.callSchema(reactAgent, input, IntentResponseSchema.class)
                : AgentUtils.callSchema(schemaAgentInvoker, input, IntentResponseSchema.class);
        return intentService.parse(schema, request);
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

    public static ReactAgent buildReactAgent(ChatModel chatModel,
                                             IntentExtractTool intentExtractTool,
                                             IntentAgentProperties properties) {
        IntentAgentProperties safeProperties = properties == null ? new IntentAgentProperties() : properties;
        String instruction = AgentUtils.loadInstruction(safeProperties.effectivePromptPath());
        return AgentUtils.reactAgentBuilder(AGENT_NAME, AGENT_DESCRIPTION, chatModel)
                .instruction(instruction)
                .methodTools(Objects.requireNonNull(intentExtractTool, "intentExtractTool must not be null"))
                .hooks(
                        new PlanHook(),
                        new AgentLogHook(),
                        new TraceHook(),
                        new ErrorHandlingHook(),
                        ModelCallLimitHook.builder()
                                .runLimit(safeProperties.effectiveRunLimit())
                                .build()
                )
                .outputKey("output")
                .outputType(IntentResponseSchema.class)
                .build();
    }
}
