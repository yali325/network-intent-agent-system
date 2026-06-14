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
            @ToolParam(required = false, description = "Search query for Huawei command knowledge. Use a short feature or command intent summary.")
            String query,
            @ToolParam(required = false, description = "Optional vendor filter, for example HUAWEI.")
            String vendor,
            @ToolParam(required = false, description = "Optional platform filter, for example VRP.")
            String platform,
            @ToolParam(required = false, description = "Optional feature filter, for example VLAN, ACL, or ROUTING.")
            String feature,
            @ToolParam(required = false, description = "Optional maximum number of documents to return. Defaults to 5 and is capped at 10.")
            Integer topK) {

        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isBlank()) {
            return new RagCommandSearchResponse(
                    List.of(),
                    List.of("RAG command knowledge search skipped because query is blank."));
        }

        int safeTopK = topK == null || topK <= 0 ? 5 : Math.min(topK, 10);
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(safeQuery)
                .topK(safeTopK)
                .similarityThresholdAll();

        var filter = buildFilter(vendor, platform, feature);
        if (filter != null) {
            builder.filterExpression(filter.build());
        }

        try {
            List<MatchedDocument> matches = vectorStoreRetriever.similaritySearch(builder.build()).stream()
                    .map(this::toMatchedDocument)
                    .toList();
            return new RagCommandSearchResponse(matches,
                    matches.isEmpty() ? List.of("No command knowledge matched the request.") : List.of());
        }
        catch (RuntimeException ex) {
            return new RagCommandSearchResponse(
                    List.of(),
                    List.of("RAG command knowledge search unavailable: " + safeExceptionSummary(ex)));
        }
    }

    private Op buildFilter(String vendor, String platform, String feature) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Op filter = null;
        filter = andIfPresent(b, filter, "vendor", vendor);
        filter = andIfPresent(b, filter, "platform", platform);
        filter = andIfPresent(b, filter, "feature", feature);
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

    private String safeExceptionSummary(RuntimeException ex) {
        String type = ex.getClass().getSimpleName();
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return type;
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        String bounded = normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
        return type + ": " + bounded;
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
