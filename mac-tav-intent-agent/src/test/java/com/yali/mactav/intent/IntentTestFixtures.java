package com.yali.mactav.intent;

import com.yali.mactav.agent.core.context.AgentRunContext;
import com.yali.mactav.intent.parser.IntentResponseParser;
import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.intent.schema.IntentResponseSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.AssumptionSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentConstraintSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentNodeSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentPreferenceSchema;
import com.yali.mactav.intent.schema.IntentResponseSchema.IntentRelationSchema;
import com.yali.mactav.model.enums.WorkflowStage;
import com.yali.mactav.model.intent.NetworkIntent;
import java.util.List;
import java.util.Map;

/**
 * Fixed offline fixtures for IntentAgent parser, validator, and service tests.
 *
 * <p>The fixture represents docs/07 enterprise-office-guest-success style data
 * without creating fake agents, mock tools, or model-call substitutes.</p>
 */
public final class IntentTestFixtures {

    public static final String TASK_ID = "task-enterprise-office-guest";

    public static final String TRACE_ID = "trace-enterprise-office-guest";

    public static final String RAW_TEXT = "Build an office and guest network policy where office can access "
            + "server, guest cannot access server, and office and guest are isolated.";

    private IntentTestFixtures() {
    }

    public static IntentAgentRequest request() {
        return IntentAgentRequest.builder()
                .taskId(TASK_ID)
                .rawText(RAW_TEXT)
                .intentVersion(2)
                .traceId(TRACE_ID)
                .userContext("enterprise office")
                .workspaceSnapshot("{}")
                .targetEnvironmentHint("lab")
                .createdBy("unit-test")
                .build();
    }

    public static AgentRunContext context() {
        return AgentRunContext.builder()
                .taskId(TASK_ID)
                .stage(WorkflowStage.INTENT)
                .version(2)
                .traceId(TRACE_ID)
                .userInput(RAW_TEXT)
                .workspaceSnapshot("{}")
                .build();
    }

    public static IntentResponseSchema enterpriseSchema() {
        return IntentResponseSchema.builder()
                .nodes(List.of(
                        node("node-office", "office", "business-zone", "Office users"),
                        node("node-guest", "guest", "business-zone", "Guest users"),
                        node("node-server", "server", "application-service", "Shared business server")
                ))
                .relations(List.of(
                        relation("rel-office-server", "access", "node-office", "node-server", "allow"),
                        relation("rel-guest-server", "access", "node-guest", "node-server", "deny"),
                        relation("rel-office-guest", "isolation", "node-office", "node-guest", "deny")
                ))
                .assumptions(List.of(
                        AssumptionSchema.builder()
                                .id("asm-default-service")
                                .field("service")
                                .value("business application access")
                                .reason("The user did not name a protocol, so keep the intent service-level.")
                                .confidence(0.72)
                                .build()
                ))
                .constraints(List.of(
                        IntentConstraintSchema.builder()
                                .id("con-guest-isolation")
                                .type("isolation")
                                .value("guest must stay isolated from office resources")
                                .description("Guest users are denied access to protected business resources.")
                                .build()
                ))
                .preferences(List.of(
                        IntentPreferenceSchema.builder()
                                .id("pref-simple-policy")
                                .type("simplicity")
                                .value("prefer simple and auditable policy")
                                .priority(1)
                                .build()
                ))
                .summary("Office can access server; guest cannot access server; office and guest are isolated.")
                .warnings(List.of("Protocol details are intentionally deferred to later stages."))
                .build();
    }

    public static NetworkIntent validIntent() {
        return new IntentResponseParser().parse(enterpriseSchema(), context());
    }

    private static IntentNodeSchema node(String id, String name, String type, String description) {
        return IntentNodeSchema.builder()
                .id(id)
                .name(name)
                .type(type)
                .description(description)
                .attributes(Map.of("businessName", name))
                .build();
    }

    private static IntentRelationSchema relation(String id, String type, String source, String target, String action) {
        return IntentRelationSchema.builder()
                .id(id)
                .type(type)
                .source(source)
                .target(target)
                .action(action)
                .service("business-application")
                .priority(1)
                .explicit(true)
                .description(source + " " + action + " " + target)
                .constraints(List.of())
                .build();
    }
}
