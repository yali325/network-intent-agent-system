package com.yali.mactav.intent.agent;

import com.yali.mactav.agent.core.context.AgentContext;
import com.yali.mactav.agent.core.result.AgentResult;
import com.yali.mactav.common.enums.StageStatus;
import com.yali.mactav.model.intent.Assumption;
import com.yali.mactav.model.intent.IntentNode;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import com.yali.mactav.model.intent.SemanticIntentGraph;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockIntentAgent implements IntentAgent {

    public static final String AGENT_NAME = "MockIntentAgent";

    @Override
    public AgentResult<NetworkIntent> execute(AgentContext context, String input) {
        String taskId = context == null ? null : context.getTaskId();
        String rawText = input != null ? input : context == null ? null : context.getRawText();
        NetworkIntent intent = buildMockIntent(taskId, rawText);
        return AgentResult.success(intent, "Mock intent parsed", AGENT_NAME, "INTENT");
    }

    public NetworkIntent buildMockIntent(String taskId, String rawText) {
        return NetworkIntent.builder()
                .taskId(taskId)
                .intentVersion(1)
                .rawText(rawText)
                .semanticIntentGraph(SemanticIntentGraph.builder()
                        .nodes(List.of(
                                node("office", "Office zone", "ZONE", "Internal office users"),
                                node("guest", "Guest zone", "ZONE", "Guest users"),
                                node("server", "Server zone", "ZONE", "Internal servers"),
                                node("internet", "Internet", "EXTERNAL_NETWORK", "External network")
                        ))
                        .relations(List.of(
                                relation("rel-001", "ACCESS", "office", "server", "ALLOW",
                                        "Office can reach server zone"),
                                relation("rel-002", "ACCESS", "guest", "server", "DENY",
                                        "Guest cannot reach server zone"),
                                relation("rel-003", "ACCESS", "office", "internet", "ALLOW",
                                        "Office can reach internet"),
                                relation("rel-004", "ACCESS", "guest", "internet", "ALLOW",
                                        "Guest can reach internet"),
                                relation("rel-005", "ISOLATION", "office", "guest", "DENY",
                                        "Office and guest zones are isolated")
                        ))
                        .build())
                .assumptions(List.of(
                        assumption("deviceTopology", "AUTO_PLAN",
                                "User did not specify a concrete topology; planning will decide it."),
                        assumption("addressPlan", "AUTO_PLAN",
                                "User did not specify address planning; planning will allocate it."),
                        assumption("vendor", "Huawei",
                                "User did not specify a vendor; Huawei-style configuration is used for demo.")
                ))
                .stageStatus(StageStatus.SUCCESS)
                .build();
    }

    private IntentNode node(String id, String name, String type, String description) {
        return IntentNode.builder()
                .id(id)
                .name(name)
                .type(type)
                .description(description)
                .build();
    }

    private IntentRelation relation(String id, String type, String source, String target, String action,
                                    String description) {
        return IntentRelation.builder()
                .id(id)
                .type(type)
                .source(source)
                .target(target)
                .action(action)
                .service("ANY")
                .description(description)
                .explicit(true)
                .build();
    }

    private Assumption assumption(String field, String value, String reason) {
        return Assumption.builder()
                .field(field)
                .value(value)
                .reason(reason)
                .build();
    }
}
