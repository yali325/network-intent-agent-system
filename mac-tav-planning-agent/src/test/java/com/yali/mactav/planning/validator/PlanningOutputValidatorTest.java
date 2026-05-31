package com.yali.mactav.planning.validator;

import static org.junit.jupiter.api.Assertions.*;

import com.yali.mactav.agent.core.validator.AgentValidationException;
import com.yali.mactav.agent.core.validator.ValidationResult;
import com.yali.mactav.model.plan.NetworkPlan;
import com.yali.mactav.planning.PlanningTestFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Offline validator tests for PlanningAgent domain and boundary rules.
 *
 * <p>The test validates DTO content only and does not use fake agents, tools,
 * ChatModel stubs, or Workspace writes.</p>
 */
class PlanningOutputValidatorTest {

    private final PlanningOutputValidator validator = new PlanningOutputValidator();

    @Test
    void validateShouldPassEnterprisePlan() {
        ValidationResult result = validator.validate(PlanningTestFixtures.validPlan());

        assertTrue(result.isValid(), () -> String.join("; ", result.getMessages()));
    }

    @Test
    void validateShouldFailWhenPlanIsNull() {
        ValidationResult result = validator.validate(null);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("not be null")));
    }

    @Test
    void validateShouldFailWhenTaskIdIsBlank() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.setTaskId(null);

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("taskId")));
    }

    @Test
    void validateShouldFailWhenTopologyIsNull() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.setTopology(null);

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("topology")));
    }

    @Test
    void validateShouldFailWhenTopologyNodesAreEmpty() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.getTopology().setNodes(new ArrayList<>());

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("nodes")));
    }

    @Test
    void validateShouldFailWhenTopologyNodeIdsAreDuplicated() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.getTopology().getNodes().get(1).setId("sw-core");

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("unique")));
    }

    @Test
    void validateShouldFailWhenSecurityPolicyActionIsInvalid() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.getSecurityPolicyPlan().get(0).setAction("REDIRECT");

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("unsupported value")));
    }

    @Test
    void validateShouldFailWhenPlanSummaryContainsCli() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.setPlanSummary("Use configure terminal and ip address 10.1.0.1 255.255.255.0");

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("CLI command content")));
    }

    @Test
    void validateShouldFailWhenTargetEnvironmentIsMissing() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.setTargetEnvironment(null);

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("targetEnvironment")));
    }

    @Test
    void validateShouldFailWhenAddressPlanIsEmpty() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.setAddressPlan(List.of());

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("addressPlan")));
    }

    @Test
    void validateShouldFailWhenVlanIdIsInvalid() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.getVlanPlan().get(0).setVlanId(4095);

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("vlanId")));
    }

    @Test
    void validateShouldFailWhenRoutingPlanIsMissing() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.setRoutingPlan(null);

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("routingPlan")));
    }

    @Test
    void validateShouldFailWhenTraceRefsAreMissing() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.setTraceRefs(null);

        ValidationResult result = validator.validate(plan);

        assertFalse(result.isValid());
        assertTrue(result.getMessages().stream().anyMatch(m -> m.contains("traceRefs")));
    }

    @Test
    void validateAndReturnShouldThrowWhenInvalid() {
        NetworkPlan plan = PlanningTestFixtures.validPlan();
        plan.setTopology(null);

        assertThrows(AgentValidationException.class, () -> validator.validateAndReturn(plan));
    }
}
