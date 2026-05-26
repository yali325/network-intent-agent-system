package com.yali.mactav.intent.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yali.mactav.agent.core.validator.AgentValidationException;
import com.yali.mactav.agent.core.validator.ValidationResult;
import com.yali.mactav.intent.IntentTestFixtures;
import com.yali.mactav.model.intent.IntentRelation;
import com.yali.mactav.model.intent.NetworkIntent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline validator tests for IntentAgent domain and boundary rules.
 *
 * <p>The test validates DTO content only and does not use fake agents, tools,
 * ChatModel stubs, or Workspace writes.</p>
 */
class IntentOutputValidatorTest {

    private final IntentOutputValidator validator = new IntentOutputValidator();

    @Test
    void validateShouldPassEnterpriseOfficeGuestIntent() {
        ValidationResult result = validator.validate(IntentTestFixtures.validIntent());

        assertTrue(result.isValid());
    }

    @Test
    void validateShouldAllowOspfAsIntentPreference() {
        NetworkIntent intent = IntentTestFixtures.validIntent();

        ValidationResult result = validator.validate(intent);

        assertTrue(result.isValid(), () -> String.join("; ", result.getMessages()));
    }

    @Test
    void validateShouldFailWhenNodesAreEmpty() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getSemanticIntentGraph().setNodes(new ArrayList<>());

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("nodes")));
    }

    @Test
    void validateShouldFailWhenRelationsAreEmpty() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getSemanticIntentGraph().setRelations(new ArrayList<>());

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("relations")));
    }

    @Test
    void validateShouldFailWhenRelationEndpointDoesNotExist() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        IntentRelation relation = intent.getSemanticIntentGraph().getRelations().get(0);
        relation.setSource("node-not-found");

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("source")));
    }

    @Test
    void validateShouldFailWhenNodeIdsAreDuplicated() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getSemanticIntentGraph().getNodes().get(1).setId("node-office");

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("node id must be unique")));
    }

    @Test
    void validateShouldFailWhenRelationIdsAreDuplicated() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getSemanticIntentGraph().getRelations().get(1).setId("rel-office-server");

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("relation id must be unique")));
    }

    @Test
    void validateShouldFailWhenNodeTypeIsImplementationObject() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getSemanticIntentGraph().getNodes().get(0).setType("ROUTER");

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("business object type")));
    }

    @Test
    void validateShouldFailWhenOutputContainsIpv4OrCidrContent() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getPreferences().get(0).setValue("OSPF with 10.10.0.0/16");

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("IP or CIDR")));
    }

    @Test
    void validateShouldFailWhenOspfAppearsAsConfigurationCommand() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getPreferences().get(0).setValue("router ospf 1 network 10.0.0.0 0.0.0.255 area 0");

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream()
                .anyMatch(message -> message.contains("outside IntentAgent boundary")));
    }

    @Test
    void validateShouldFailWhenRoutingProtocolAppearsInBusinessObjectField() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getSemanticIntentGraph().getNodes().get(0).setName("ospf-area");

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream()
                .anyMatch(message -> message.contains("routing protocol content")));
    }

    @Test
    void validateShouldFailWhenOutputContainsPlanningOrConfigurationContent() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getSemanticIntentGraph().getNodes().get(0).setType("vlan");
        intent.getSemanticIntentGraph().getRelations().get(0)
                .setDescription("configure interface GigabitEthernet0/1 with ip address 10.0.0.1");

        ValidationResult result = validator.validate(intent);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream()
                .anyMatch(message -> message.contains("outside IntentAgent boundary")));
    }

    @Test
    void validateAndReturnShouldThrowAgentValidationExceptionWhenInvalid() {
        NetworkIntent intent = IntentTestFixtures.validIntent();
        intent.getSemanticIntentGraph().setRelations(List.of());

        assertThrows(AgentValidationException.class, () -> validator.validateAndReturn(intent));
    }
}
