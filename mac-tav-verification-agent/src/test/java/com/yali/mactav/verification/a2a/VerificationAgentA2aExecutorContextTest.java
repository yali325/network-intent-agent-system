package com.yali.mactav.verification.a2a;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.alibaba.cloud.ai.a2a.autoconfigure.server.A2aServerAgentCardAutoConfiguration;
import com.alibaba.cloud.ai.a2a.autoconfigure.server.A2aServerHandlerAutoConfiguration;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yali.mactav.model.enums.StageStatus;
import com.yali.mactav.model.enums.ValidationStatus;
import com.yali.mactav.model.verification.ValidationReport;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verifies that the official SAA A2A request handler uses the custom Verification executor.
 */
class VerificationAgentA2aExecutorContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(A2aServerHandlerAutoConfiguration.class))
            .withUserConfiguration(CustomExecutorConfiguration.class);

    private final ApplicationContextRunner agentCardContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(A2aServerAgentCardAutoConfiguration.class))
            .withUserConfiguration(AgentOnlyConfiguration.class)
            .withPropertyValues(
                    "spring.ai.alibaba.a2a.server.version=1.0.0",
                    "spring.ai.alibaba.a2a.server.card.name=VerificationAgent",
                    "spring.ai.alibaba.a2a.server.card.description=Verification agent test card",
                    "spring.ai.alibaba.a2a.server.card.capabilities.streaming=false",
                    "spring.ai.alibaba.a2a.server.card.capabilities.push-notifications=false",
                    "spring.ai.alibaba.a2a.server.card.capabilities.state-transition-history=false");

    @Test
    void requestHandlerShouldUseCustomVerificationExecutorAndNotGraphExecutor() {
        contextRunner.run(context -> {
            Map<String, AgentExecutor> executors = context.getBeansOfType(AgentExecutor.class);
            assertEquals(1, executors.size());
            AgentExecutor executor = context.getBean(AgentExecutor.class);
            assertInstanceOf(VerificationAgentA2aExecutor.class, executor);
            assertFalse(executors.values().stream()
                    .anyMatch(candidate -> "com.alibaba.cloud.ai.a2a.core.server.GraphAgentExecutor"
                            .equals(candidate.getClass().getName())));

            RequestHandler requestHandler = context.getBean(RequestHandler.class);
            assertSame(executor, readAgentExecutor(requestHandler));
        });
    }

    @Test
    void serverCardCapabilitiesShouldBindNonStreaming() {
        agentCardContextRunner.run(context -> {
            AgentCard agentCard = context.getBean(AgentCard.class);
            assertFalse(agentCard.capabilities().streaming());
            assertFalse(agentCard.capabilities().pushNotifications());
            assertFalse(agentCard.capabilities().stateTransitionHistory());
        });
    }

    private AgentExecutor readAgentExecutor(RequestHandler requestHandler) throws Exception {
        Field field = requestHandler.getClass().getDeclaredField("agentExecutor");
        field.setAccessible(true);
        return (AgentExecutor) field.get(requestHandler);
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomExecutorConfiguration {

        @Bean(name = "agentExecutor")
        AgentExecutor agentExecutor() {
            return new VerificationAgentA2aExecutor(request -> ValidationReport.builder()
                    .validationId("validation-" + request.getTaskId() + "-v1")
                    .taskId(request.getTaskId())
                    .executionId("execution-" + request.getTaskId() + "-v1")
                    .validationVersion(request.getValidationVersion())
                    .overallStatus(ValidationStatus.PASSED)
                    .summary("validated")
                    .stageStatus(StageStatus.SUCCESS)
                    .createTime(LocalDateTime.now())
                    .build(), new ObjectMapper());
        }

        @Bean
        AgentCard agentCard() {
            return new AgentCard(
                    "VerificationAgent",
                    "Verification agent test card",
                    "http://localhost/a2a/message",
                    null,
                    "1.0.0",
                    null,
                    new AgentCapabilities(false, false, false, List.of()),
                    List.of("text"),
                    List.of("text"),
                    List.of(),
                    false,
                    Map.of(),
                    List.of(),
                    null,
                    List.of(),
                    null,
                    "0.2.5");
        }

        @Bean
        Agent rootAgent() {
            return new TestOnlyAgent();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class AgentOnlyConfiguration {

        @Bean
        Agent rootAgent() {
            return new TestOnlyAgent();
        }
    }

    private static final class TestOnlyAgent extends Agent {

        private TestOnlyAgent() {
            super("VerificationAgent", "Test-only root agent for SAA auto-configuration");
        }

        @Override
        protected StateGraph initGraph() throws GraphStateException {
            throw new GraphStateException("Test-only agent must not be executed");
        }
    }
}
