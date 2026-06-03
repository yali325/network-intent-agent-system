package com.yali.mactav.verification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for running VerificationAgent as an A2A service.
 */
@SpringBootApplication
public class VerificationAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(VerificationAgentApplication.class, args);
    }
}
