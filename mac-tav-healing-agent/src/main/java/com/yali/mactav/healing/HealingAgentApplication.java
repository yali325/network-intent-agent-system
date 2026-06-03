package com.yali.mactav.healing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the real MAC-TAV HealingAgent service.
 */
@SpringBootApplication
public class HealingAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealingAgentApplication.class, args);
    }
}
