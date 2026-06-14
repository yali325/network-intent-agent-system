package com.yali.mactav.configuration.tool;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;

/**
 * Offline tests for read-only RagCommandSearchTool.
 */
class RagCommandSearchToolTest {

    @Test
    void shouldReturnMatchesWithMetadataAndScore() {
        RecordingRetriever retriever = new RecordingRetriever(List.of(Document.builder()
                .id("ready-vlan-fixture")
                .text("Fixture content for retrieval.")
                .metadata(metadata())
                .score(0.87)
                .build()));
        RagCommandSearchTool tool = new RagCommandSearchTool(retriever);

        var response = tool.searchCommandKnowledge("vlan isolation", "HUAWEI", "VRP", "VLAN", 3);

        assertEquals(1, retriever.calls);
        assertEquals("vlan isolation", retriever.requests.get(0).getQuery());
        assertEquals(3, retriever.requests.get(0).getTopK());
        assertEquals(1, response.matchedDocuments().size());
        var match = response.matchedDocuments().get(0);
        assertEquals("ready-vlan-fixture", match.documentId());
        assertEquals("Ready VLAN Fixture", match.title());
        assertEquals("RAG", match.suggestedGenerationSourceType());
        assertEquals("ready-vlan-fixture", match.suggestedSourceId());
        assertEquals(0.87, match.score());
        assertEquals("HUAWEI", match.metadata().get("vendor"));
    }

    @Test
    void shouldReturnWarningForEmptyResults() {
        RecordingRetriever retriever = new RecordingRetriever(List.of());
        RagCommandSearchTool tool = new RagCommandSearchTool(retriever);

        var response = tool.searchCommandKnowledge("unknown", "HUAWEI", "VRP", "UNKNOWN", 2);

        assertTrue(response.matchedDocuments().isEmpty());
        assertFalse(response.warnings().isEmpty());
        assertEquals(1, retriever.calls);
    }

    @Test
    void shouldClampInvalidTopK() {
        RecordingRetriever retriever = new RecordingRetriever(List.of());
        RagCommandSearchTool tool = new RagCommandSearchTool(retriever);

        tool.searchCommandKnowledge("vlan", "HUAWEI", "VRP", "VLAN", 0);

        assertEquals(5, retriever.requests.get(0).getTopK());
    }

    @Test
    void shouldNotAddOptionalFiltersWhenFilterValuesAreBlank() {
        RecordingRetriever retriever = new RecordingRetriever(List.of());
        RagCommandSearchTool tool = new RagCommandSearchTool(retriever);

        tool.searchCommandKnowledge("acl command", null, "", "  ", 3);

        assertEquals(1, retriever.calls);
        assertEquals("acl command", retriever.requests.get(0).getQuery());
        assertEquals(3, retriever.requests.get(0).getTopK());
    }

    @Test
    void shouldReturnWarningWhenQueryIsBlankWithoutCallingRetriever() {
        RecordingRetriever retriever = new RecordingRetriever(List.of());
        RagCommandSearchTool tool = new RagCommandSearchTool(retriever);

        var response = tool.searchCommandKnowledge(" ", null, null, null, null);

        assertTrue(response.matchedDocuments().isEmpty());
        assertFalse(response.warnings().isEmpty());
        assertEquals(0, retriever.calls);
    }

    @Test
    void shouldReturnWarningWhenRetrieverFails() {
        FailingRetriever retriever = new FailingRetriever();
        RagCommandSearchTool tool = new RagCommandSearchTool(retriever);

        var response = tool.searchCommandKnowledge("vlan command", null, null, null, 3);

        assertTrue(response.matchedDocuments().isEmpty());
        assertEquals(1, retriever.calls);
        assertTrue(response.warnings().stream().anyMatch(warning -> warning.contains("unavailable")));
    }

    private Map<String, Object> metadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", "ready-vlan-fixture");
        metadata.put("title", "Ready VLAN Fixture");
        metadata.put("vendor", "HUAWEI");
        metadata.put("platform", "VRP");
        metadata.put("feature", "VLAN");
        metadata.put("generationSourceType", "RAG");
        metadata.put("status", "READY");
        return metadata;
    }

    private static final class RecordingRetriever implements VectorStoreRetriever {

        private final List<Document> documents;

        private final List<SearchRequest> requests = new ArrayList<>();

        private int calls;

        private RecordingRetriever(List<Document> documents) {
            this.documents = documents;
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            calls++;
            requests.add(request);
            return documents;
        }
    }

    private static final class FailingRetriever implements VectorStoreRetriever {

        private int calls;

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            calls++;
            throw new IllegalStateException("collection not ready");
        }
    }
}
