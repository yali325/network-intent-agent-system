package com.yali.mactav.intent.manual;

import com.yali.mactav.intent.agent.IntentAgent;
import com.yali.mactav.intent.config.IntentAgentConfiguration;
import com.yali.mactav.intent.request.IntentAgentRequest;
import com.yali.mactav.model.intent.NetworkIntent;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * One-shot manual validation entry for the real DashScope-backed IntentAgent.
 *
 * <p>This class is under test sources and is not executed by mvn test. Run it
 * manually only after setting ALI_API_KEY as an environment variable. It must
 * not be used as an automated test or Web entry point.</p>
 */
public final class IntentAgentManualValidation {

    private static final String DEFAULT_INPUT = "Build an office and guest network policy. "
            + "Office users can access the server. Guest users cannot access the server. "
            + "Office and guest users must be isolated from each other. Use OSPF as the preferred routing protocol.";

    private IntentAgentManualValidation() {
    }

    public static void main(String[] args) {
        String apiKey = System.getenv("ALI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set ALI_API_KEY before manual validation.");
        }
        String rawText = args == null || args.length == 0 ? DEFAULT_INPUT : String.join(" ", args);

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ManualIntentAgentApplication.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "spring.ai.dashscope.api-key=" + apiKey,
                        "spring.ai.dashscope.chat.options.model=qwen-plus",
                        "mactav.agents.intent.manual-model-validation-enabled=true")
                .run(args)) {
            IntentAgent intentAgent = context.getBean(IntentAgent.class);
            NetworkIntent intent = intentAgent.run(IntentAgentRequest.builder()
                    .taskId("manual-intent-" + System.currentTimeMillis())
                    .intentVersion(1)
                    .traceId("manual-trace-" + System.currentTimeMillis())
                    .rawText(rawText)
                    .createdBy("manual-validation")
                    .build());

            System.out.println("Manual IntentAgent validation completed.");
            System.out.println("taskId=" + intent.getTaskId());
            System.out.println("nodes=" + intent.getSemanticIntentGraph().getNodes().size());
            System.out.println("relations=" + intent.getSemanticIntentGraph().getRelations().size());
            System.out.println("preferences=" + intent.getPreferences().size());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(IntentAgentConfiguration.class)
    static class ManualIntentAgentApplication {
    }
}
