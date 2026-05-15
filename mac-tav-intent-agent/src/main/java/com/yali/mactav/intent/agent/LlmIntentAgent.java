package com.yali.mactav.intent.agent;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.llm.LlmJsonParser;
import com.yali.mactav.agent.core.llm.LlmPromptRunner;
import com.yali.mactav.agent.core.llm.PromptTemplateLoader;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.intent.SemanticIntentGraph;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LlmIntentAgent implements IntentAgent {

    public static final String AGENT_NAME = "LlmIntentAgent";
    private static final String STAGE = "INTENT";
    private static final String PROMPT_LOCATION = "classpath:prompts/intent-system-prompt.md";
    private static final Pattern IPV4_PATTERN = Pattern.compile("\\b\\d{1,3}(?:\\.\\d{1,3}){3}(?:/\\d{1,2})?\\b");
    private static final Pattern INTERFACE_PATTERN = Pattern.compile("\\b(?:GE|GigabitEthernet|Ethernet|eth)\\d+(?:/\\d+)*(?:\\.\\d+)?\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("\\b(?:R\\d+|SW\\d+|AR\\d+|CE\\d+)\\b",
            Pattern.CASE_INSENSITIVE);

    private final LlmPromptRunner promptRunner;
    private final LlmJsonParser jsonParser;
    private final PromptTemplateLoader promptTemplateLoader;

    public LlmIntentAgent(LlmPromptRunner promptRunner,
                          LlmJsonParser jsonParser,
                          PromptTemplateLoader promptTemplateLoader) {
        this.promptRunner = promptRunner;
        this.jsonParser = jsonParser;
        this.promptTemplateLoader = promptTemplateLoader;
    }

    @Override
    public AgentResult<NetworkIntent> execute(AgentContext context, String input) {
        try {
            String taskId = context == null ? null : context.getTaskId();
            String rawText = input != null ? input : context == null ? null : context.getRawText();
            if (isBlank(rawText)) {
                return AgentResult.failure("rawText must not be blank", ErrorCode.BAD_REQUEST.name(), AGENT_NAME, STAGE);
            }

            String systemPrompt = promptTemplateLoader.load(PROMPT_LOCATION);
            String llmOutput = promptRunner.run(systemPrompt, userPrompt(taskId, rawText));
            NetworkIntent intent = jsonParser.parseObject(llmOutput, NetworkIntent.class);
            normalizeIntent(intent, taskId, rawText);
            validateIntent(intent);
            return AgentResult.success(intent, "LLM intent parsed", AGENT_NAME, STAGE);
        } catch (BusinessException ex) {
            return AgentResult.failure(ex.getMessage(), ex.getErrorCode().name(), AGENT_NAME, STAGE);
        } catch (Exception ex) {
            return AgentResult.failure(readableMessage(ex), ErrorCode.PIPELINE_FAILED.name(), AGENT_NAME, STAGE);
        }
    }

    public void validateIntent(NetworkIntent intent) {
        if (intent == null) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "NetworkIntent is null");
        }
        SemanticIntentGraph graph = intent.getSemanticIntentGraph();
        if (graph == null) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "semanticIntentGraph must not be null");
        }
        List<IntentNode> nodes = graph.getNodes();
        List<IntentRelation> relations = graph.getRelations();
        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "semanticIntentGraph.nodes must not be empty");
        }
        if (relations == null || relations.isEmpty()) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "semanticIntentGraph.relations must not be empty");
        }

        Set<String> nodeIds = new HashSet<>();
        for (IntentNode node : nodes) {
            if (node == null || isBlank(node.getId())) {
                throw new BusinessException(ErrorCode.PIPELINE_FAILED, "intent node id must not be blank");
            }
            if (containsNetworkDesign(node.getId(), node.getName(), node.getType(), node.getDescription())) {
                throw new BusinessException(
                        ErrorCode.PIPELINE_FAILED,
                        "NetworkIntent must not contain concrete device, interface, VLAN, or IP details"
                );
            }
            nodeIds.add(node.getId());
        }

        for (IntentRelation relation : relations) {
            if (relation == null || isBlank(relation.getId())) {
                throw new BusinessException(ErrorCode.PIPELINE_FAILED, "intent relation id must not be blank");
            }
            if (!nodeIds.contains(relation.getSource())) {
                throw new BusinessException(
                        ErrorCode.PIPELINE_FAILED,
                        "relation.source does not exist in nodes: " + relation.getSource()
                );
            }
            if (!nodeIds.contains(relation.getTarget())) {
                throw new BusinessException(
                        ErrorCode.PIPELINE_FAILED,
                        "relation.target does not exist in nodes: " + relation.getTarget()
                );
            }
            if (containsNetworkDesign(
                    relation.getId(),
                    relation.getType(),
                    relation.getSource(),
                    relation.getTarget(),
                    relation.getAction(),
                    relation.getService(),
                    relation.getDescription())) {
                throw new BusinessException(
                        ErrorCode.PIPELINE_FAILED,
                        "NetworkIntent relations must not contain concrete device, interface, VLAN, or IP details"
                );
            }
        }
    }

    private String userPrompt(String taskId, String rawText) {
        return """
                taskId: %s
                rawText: %s

                请基于 rawText 输出 NetworkIntent JSON。只能输出 JSON 对象本身。
                """.formatted(nullToEmpty(taskId), rawText);
    }

    private void normalizeIntent(NetworkIntent intent, String taskId, String rawText) {
        if (intent.getTaskId() == null || intent.getTaskId().isBlank()) {
            intent.setTaskId(taskId);
        }
        if (intent.getIntentVersion() == null) {
            intent.setIntentVersion(1);
        }
        if (intent.getRawText() == null || intent.getRawText().isBlank()) {
            intent.setRawText(rawText);
        }
        intent.setStageStatus(StageStatus.SUCCESS);
    }

    private boolean containsNetworkDesign(String... values) {
        for (String value : values) {
            if (value == null) {
                continue;
            }
            if (IPV4_PATTERN.matcher(value).find()
                    || INTERFACE_PATTERN.matcher(value).find()
                    || DEVICE_ID_PATTERN.matcher(value).find()
                    || value.toUpperCase().contains("VLAN")
                    || value.toUpperCase().contains("CLI")) {
                return true;
            }
        }
        return false;
    }

    private String readableMessage(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return ex.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
