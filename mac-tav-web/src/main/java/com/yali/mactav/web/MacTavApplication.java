package com.yali.mactav.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot entry point for the MAC-TAV Web/API service.
 *
 * <p>The Web application scans Web, Orchestrator, Model Core, and Execution
 * packages only. It must not scan or directly wire concrete professional agent
 * modules such as mac-tav-intent-agent.</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.yali.mactav.web",
        "com.yali.mactav.orchestrator",
        "com.yali.mactav.modelcore",
        "com.yali.mactav.execution"
})
public class MacTavApplication {

    public static void main(String[] args) {
        SpringApplication.run(MacTavApplication.class, args);
    }
}
