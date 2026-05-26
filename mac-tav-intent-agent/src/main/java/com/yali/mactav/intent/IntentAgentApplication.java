package com.yali.mactav.intent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone Spring Boot entry point for the IntentAgent professional service.
 *
 * <p>This application exposes only internal agent-service endpoints and must
 * not be used as the MAC-TAV frontend Web API or workflow orchestrator.</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.yali.mactav.intent"
})
public class IntentAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntentAgentApplication.class, args);
    }
}
