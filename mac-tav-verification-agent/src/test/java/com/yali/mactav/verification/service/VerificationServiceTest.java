package com.yali.mactav.verification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.agent.core.validator.AgentValidationException;
import com.yali.mactav.model.enums.ValidationStatus;
import com.yali.mactav.model.verification.ValidationReport;
import com.yali.mactav.verification.request.VerificationAgentRequest;
import com.yali.mactav.verification.schema.VerificationResponseSchema;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline VerificationService tests for parser and validator behavior.
 */
class VerificationServiceTest {

    @Test
    void parseShouldCreateValidationReportWithTraceableItems() {
        VerificationService service = new VerificationServiceImpl();
        VerificationResponseSchema schema = VerificationResponseSchema.builder()
                .overallStatus(ValidationStatus.PASSED)
                .summary("All intent relations are satisfied by execution facts.")
                .items(List.of(VerificationResponseSchema.ValidationItemSchema.builder()
                        .itemId("val-rel-office-server")
                        .name("office to server reachability")
                        .type("connectivity")
                        .expected("REACHABLE")
                        .actual("REACHABLE")
                        .passed(true)
                        .severity("LOW")
                        .relatedIntentRelationId("rel-office-server")
                        .relatedPlanElementIds(List.of("policy-office-server"))
                        .relatedConfigBlockIds(List.of("cfg-policy-office-server"))
                        .relatedTestId("test-office-server")
                        .evidenceIds(List.of("evidence-office-server"))
                        .message("The execution ping test matches the allowed access intent.")
                        .build()))
                .evidences(List.of(VerificationResponseSchema.ValidationEvidenceSchema.builder()
                        .evidenceId("evidence-office-server")
                        .evidenceType("TEST_RESULT")
                        .source("ExecutionReport")
                        .rawValue("PASSED")
                        .normalizedValue("REACHABLE")
                        .relatedTestId("test-office-server")
                        .build()))
                .suggestions(List.of("No repair action is required."))
                .build();

        ValidationReport report = service.parse(schema, request());

        assertEquals("task-verification-test", report.getTaskId());
        assertEquals(1, report.getValidationVersion());
        assertEquals(ValidationStatus.PASSED, report.getOverallStatus());
        assertEquals("rel-office-server", report.getItems().get(0).getRelatedIntentRelationId());
        assertTrue(report.getTraceRefs().getValidationItemIds().contains("val-rel-office-server"));
        assertTrue(report.getTraceRefs().getTestIds().contains("test-office-server"));
    }

    @Test
    void parseShouldRejectVerificationOverreach() {
        VerificationService service = new VerificationServiceImpl();
        VerificationResponseSchema schema = VerificationResponseSchema.builder()
                .overallStatus(ValidationStatus.FAILED)
                .summary("Run mininet again and apply config now.")
                .items(List.of(VerificationResponseSchema.ValidationItemSchema.builder()
                        .itemId("val-overreach")
                        .expected("BLOCKED")
                        .actual("REACHABLE")
                        .passed(false)
                        .relatedTestId("test-guest-server")
                        .build()))
                .build();

        assertThrows(AgentValidationException.class, () -> service.parse(schema, request()));
    }

    private VerificationAgentRequest request() {
        return VerificationAgentRequest.builder()
                .taskId("task-verification-test")
                .rawText("office can access server")
                .intentVersion(1)
                .planVersion(1)
                .configVersion(1)
                .executionVersion(1)
                .validationVersion(1)
                .traceId("trace-verification-test")
                .build();
    }
}
