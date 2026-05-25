package com.yali.mactav.intent.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured model-output boundary for MAC-TAV IntentAgent.
 *
 * <p>This schema is parsed into NetworkIntent before validation. It must only
 * describe business objects, access/isolation relations, assumptions,
 * constraints, and preferences; it must not include device, interface, VLAN,
 * IP, topology, routing-plan, ACL, or CLI configuration fields.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResponseSchema {

    @Builder.Default
    private List<IntentNodeSchema> nodes = new ArrayList<>();

    @Builder.Default
    private List<IntentRelationSchema> relations = new ArrayList<>();

    @Builder.Default
    private List<AssumptionSchema> assumptions = new ArrayList<>();

    @Builder.Default
    private List<IntentConstraintSchema> constraints = new ArrayList<>();

    @Builder.Default
    private List<IntentPreferenceSchema> preferences = new ArrayList<>();

    private String summary;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Schema representation of one business object recognized in user intent.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentNodeSchema {

        private String id;

        private String name;

        private String type;

        private String description;

        @Builder.Default
        private Map<String, Object> attributes = new HashMap<>();
    }

    /**
     * Schema representation of an access or isolation relation between nodes.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentRelationSchema {

        private String id;

        private String type;

        private String source;

        private String target;

        private String action;

        private String service;

        private Integer priority;

        private Boolean explicit;

        private String description;

        @Builder.Default
        private List<IntentConstraintSchema> constraints = new ArrayList<>();
    }

    /**
     * Schema representation of an assumption made while interpreting intent.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssumptionSchema {

        private String id;

        private String field;

        private String value;

        private String reason;

        private Double confidence;
    }

    /**
     * Schema representation of an intent-level constraint.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentConstraintSchema {

        private String id;

        private String type;

        private String value;

        private String description;
    }

    /**
     * Schema representation of an intent-level preference.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntentPreferenceSchema {

        private String id;

        private String type;

        private String value;

        private Integer priority;
    }
}
