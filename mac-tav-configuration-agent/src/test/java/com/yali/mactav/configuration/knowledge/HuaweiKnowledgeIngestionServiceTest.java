package com.yali.mactav.configuration.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

/**
 * Offline tests for Huawei Markdown parsing and explicit VectorStore ingestion.
 */
class HuaweiKnowledgeIngestionServiceTest {

    @Test
    void parserShouldReadFrontMatterAndBody() {
        String markdown = """
                ---
                id: parser-fixture
                title: Parser Fixture
                vendor: HUAWEI
                platform: VRP
                feature: VLAN
                tags: [fixture, parser]
                knowledgeType: COMMAND_REFERENCE
                generationSourceType: RAG
                riskLevel: LOW
                status: READY
                version: 0.1.0
                updatedAt: 2026-05-31
                ---

                ## 用途

                Parser body.
                """;

        HuaweiKnowledgeDocument document = new HuaweiKnowledgeMarkdownParser().parse(markdown);

        assertEquals("parser-fixture", document.id());
        assertEquals("HUAWEI", document.vendor());
        assertEquals(List.of("fixture", "parser"), document.tags());
        assertTrue(document.body().contains("Parser body"));
    }

    @Test
    void ingestionShouldAddOnlyReadyNonPlaceholderDocuments() {
        RecordingVectorStore vectorStore = new RecordingVectorStore();
        HuaweiKnowledgeIngestionService service = new HuaweiKnowledgeIngestionService(vectorStore);

        var result = service.ingest("classpath*:knowledge/huawei/*.md");

        assertEquals(1, result.ingestedCount());
        assertEquals(1, vectorStore.addCalls);
        assertEquals(1, vectorStore.addedDocuments.size());
        assertEquals("ready-vlan-fixture", vectorStore.addedDocuments.get(0).getId());
        assertEquals("HUAWEI", vectorStore.addedDocuments.get(0).getMetadata().get("vendor"));
        assertTrue(result.skippedDocumentIds().contains("draft-fixture"));
        assertTrue(result.skippedDocumentIds().contains("todo-ready-fixture"));
    }

    private static final class RecordingVectorStore implements VectorStore {

        private final List<Document> addedDocuments = new ArrayList<>();

        private int addCalls;

        @Override
        public void add(List<Document> documents) {
            addCalls++;
            addedDocuments.addAll(documents);
        }

        @Override
        public void delete(List<String> idList) {
            throw new UnsupportedOperationException("delete should not be called");
        }

        @Override
        public void delete(Filter.Expression filterExpression) {
            throw new UnsupportedOperationException("delete should not be called");
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return List.of();
        }
    }
}
