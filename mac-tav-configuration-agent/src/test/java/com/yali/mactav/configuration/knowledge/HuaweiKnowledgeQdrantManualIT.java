package com.yali.mactav.configuration.knowledge;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Manual Qdrant ingestion entry for Huawei command knowledge documents.
 */
@EnabledIfEnvironmentVariable(named = "MACTAV_RUN_HUAWEI_KB_INGEST", matches = "true")
class HuaweiKnowledgeQdrantManualIT {

    @Test
    void ingestHuaweiKnowledgeIntoQdrant() {
        SpringApplication application = new SpringApplication(QdrantIngestionTestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setDefaultProperties(java.util.Map.of(
                "spring.main.banner-mode", "off",
                "spring.ai.dashscope.api-key",
                "${SPRING_AI_DASHSCOPE_API_KEY:${ALI_API_KEY:${DASHSCOPE_API_KEY:${aliApi-key:}}}}",
                "spring.ai.vectorstore.qdrant.host", "${MACTAV_QDRANT_HOST:127.0.0.1}",
                "spring.ai.vectorstore.qdrant.port", "${MACTAV_QDRANT_PORT:16334}",
                "spring.ai.vectorstore.qdrant.collection-name",
                "${MACTAV_QDRANT_COLLECTION:mactav_huawei_command_kb}",
                "spring.ai.vectorstore.qdrant.use-tls", "false",
                "spring.ai.vectorstore.qdrant.initialize-schema", "true",
                "spring.ai.alibaba.a2a.nacos.discovery.enabled", "false",
                "spring.ai.alibaba.a2a.nacos.registry.enabled", "false"
        ));

        try (ConfigurableApplicationContext context = application.run()) {
            HuaweiKnowledgeIngestionService service = context.getBean(HuaweiKnowledgeIngestionService.class);
            HuaweiKnowledgeIngestionService.IngestionResult result =
                    service.ingest("file:src/main/resources/knowledge/huawei/*.md");

            assertTrue(result.ingestedCount() >= 6,
                    () -> "Expected at least 6 READY Huawei command documents, but ingested "
                            + result.ingestedCount());
        }
    }

    @SpringBootApplication(scanBasePackageClasses = HuaweiKnowledgeIngestionService.class)
    static class QdrantIngestionTestApplication {
    }
}
