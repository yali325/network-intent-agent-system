package com.yali.mactav.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone Spring Boot entry point for the ConfigurationAgent service.
 */
@SpringBootApplication(scanBasePackages = {
        "com.yali.mactav.configuration"
})
public class ConfigurationAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationAgentApplication.class, args);
    }
}
