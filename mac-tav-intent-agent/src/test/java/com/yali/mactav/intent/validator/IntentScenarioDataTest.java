package com.yali.mactav.intent.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yali.mactav.agent.core.validator.ValidationResult;
import com.yali.mactav.model.intent.NetworkIntent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Offline regression test for docs/test-data Intent-stage scenario JSON.
 *
 * <p>The sample is fixed parser/validator data only. It does not call a model,
 * create a fake agent, or replace the long-term A2A service path.</p>
 */
class IntentScenarioDataTest {

    private static final Path SCENARIO_INTENT_PATH = Path.of(
            "docs",
            "test-data",
            "scenarios",
            "enterprise-office-guest-success",
            "expected-intent.json"
    );

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final IntentOutputValidator validator = new IntentOutputValidator();

    @Test
    void expectedIntentJsonShouldDeserializeAndPassValidator() throws IOException {
        NetworkIntent intent = objectMapper.readValue(resolveScenarioPath().toFile(), NetworkIntent.class);

        ValidationResult result = validator.validate(intent);

        assertTrue(result.isValid(), () -> String.join("; ", result.getMessages()));
        assertEquals("OSPF", intent.getPreferences().get(0).getValue());
    }

    private Path resolveScenarioPath() {
        if (Files.exists(SCENARIO_INTENT_PATH)) {
            return SCENARIO_INTENT_PATH;
        }
        return Path.of("..").resolve(SCENARIO_INTENT_PATH).normalize();
    }
}
