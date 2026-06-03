package com.yali.mactav.verification.schema;

import com.yali.mactav.model.enums.ValidationStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured model-output boundary for MAC-TAV VerificationAgent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseSchema {

    private ValidationStatus overallStatus;

    private String summary;

    @Builder.Default
    private List<ValidationItemSchema> items = new ArrayList<>();

    @Builder.Default
    private List<ValidationEvidenceSchema> evidences = new ArrayList<>();

    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    /**
     * Schema representation of one intent satisfaction check.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationItemSchema {

        private String itemId;

        private String name;

        private String type;

        private String expected;

        private String actual;

        private Boolean passed;

        private String severity;

        private String relatedIntentRelationId;

        @Builder.Default
        private List<String> relatedPlanElementIds = new ArrayList<>();

        @Builder.Default
        private List<String> relatedConfigBlockIds = new ArrayList<>();

        private String relatedTestId;

        @Builder.Default
        private List<String> evidenceIds = new ArrayList<>();

        private String message;
    }

    /**
     * Schema representation of execution evidence cited by validation items.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationEvidenceSchema {

        private String evidenceId;

        private String evidenceType;

        private String source;

        private String rawValue;

        private String normalizedValue;

        @Builder.Default
        private Map<String, Object> metadata = new HashMap<>();

        private String relatedTestId;

        private String relatedRuntimeObjectId;
    }
}
