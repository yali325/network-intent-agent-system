package com.yali.mactav.configuration.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder.Op;
import org.springframework.stereotype.Component;

/**
 * Spring AI method tool for read-only Agentic RAG command knowledge search.
 */
@Component
public class RagCommandSearchTool {

    private final VectorStoreRetriever vectorStoreRetriever;

    public RagCommandSearchTool(VectorStoreRetriever vectorStoreRetriever) {
        this.vectorStoreRetriever = vectorStoreRetriever;
    }

    @Tool(name = "searchHuaweiCommandKnowledge",
            description = "Search Huawei command knowledge using VectorStore similarity search. Read-only; does not execute commands, write workspace state, or mutate the vector store.")
    public RagCommandSearchResponse searchCommandKnowledge(
            @ToolParam(required = true, description = "RAG search request with query, vendor, platform, feature, and topK.") RagCommandSearchRequest request) {

        RagCommandSearchRequest safeRequest = request == null
                ? new RagCommandSearchRequest("", null, null, null, 5)
                : request;
        int topK = safeRequest.topK() == null || safeRequest.topK() <= 0 ? 5 : Math.min(safeRequest.topK(), 10);
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(safeRequest.query() == null ? "" : safeRequest.query())
                .topK(topK)
                .similarityThresholdAll();

        var filter = buildFilter(safeRequest);
        if (filter != null) {
            builder.filterExpression(filter.build());
        }

        List<MatchedDocument> matches = vectorStoreRetriever.similaritySearch(builder.build()).stream()
                .map(this::toMatchedDocument)
                .toList();
        return new RagCommandSearchResponse(matches,
                matches.isEmpty() ? List.of("No command knowledge matched the request.") : List.of());
    }

    private Op buildFilter(RagCommandSearchRequest request) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Op filter = null;
        filter = andIfPresent(b, filter, "vendor", request.vendor());
        filter = andIfPresent(b, filter, "platform", request.platform());
        filter = andIfPresent(b, filter, "feature", request.feature());
        filter = andIfPresent(b, filter, "status", "READY");
        return filter;
    }

    private Op andIfPresent(FilterExpressionBuilder builder, Op current, String key, String value) {
        if (value == null || value.isBlank()) {
            return current;
        }
        Op next = builder.eq(key, value);
        return current == null ? next : builder.and(current, next);
    }

    private MatchedDocument toMatchedDocument(Document document) {
        Map<String, Object> metadata = new LinkedHashMap<>(document.getMetadata());
        String documentId = String.valueOf(metadata.getOrDefault("id", document.getId()));
        String title = String.valueOf(metadata.getOrDefault("title", ""));
        return new MatchedDocument(
                documentId,
                title,
                snippet(document.getText()),
                metadata,
                document.getScore(),
                "RAG",
                documentId);
    }

    private String snippet(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240);
    }

    /**
     * Request for read-only RAG command knowledge search.
     */
    public record RagCommandSearchRequest(
            String query,
            String vendor,
            String platform,
            String feature,
            Integer topK) {
    }

    /**
     * Response containing matched command knowledge documents.
     */
    public record RagCommandSearchResponse(
            List<MatchedDocument> matchedDocuments,
            List<String> warnings) {
    }

    /**
     * One vector-search match prepared for GenerationSource construction.
     */
    public record MatchedDocument(
            String documentId,
            String title,
            String contentSnippet,
            Map<String, Object> metadata,
            Double score,
            String suggestedGenerationSourceType,
            String suggestedSourceId) {
    }
}
