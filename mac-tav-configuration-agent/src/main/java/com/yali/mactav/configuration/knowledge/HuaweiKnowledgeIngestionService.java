package com.yali.mactav.configuration.knowledge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Explicit ingestion service for Huawei Markdown knowledge documents.
 */
@Component
public class HuaweiKnowledgeIngestionService {

    public static final String DEFAULT_RESOURCE_PATTERN = "classpath*:knowledge/huawei/*.md";

    private final VectorStore vectorStore;

    private final ResourcePatternResolver resourceResolver;

    private final HuaweiKnowledgeMarkdownParser parser;

    @Autowired
    public HuaweiKnowledgeIngestionService(VectorStore vectorStore) {
        this(vectorStore, new PathMatchingResourcePatternResolver(), new HuaweiKnowledgeMarkdownParser());
    }

    HuaweiKnowledgeIngestionService(VectorStore vectorStore,
                                    ResourcePatternResolver resourceResolver,
                                    HuaweiKnowledgeMarkdownParser parser) {
        this.vectorStore = vectorStore;
        this.resourceResolver = resourceResolver;
        this.parser = parser;
    }

    public IngestionResult ingestHuaweiKnowledgeBase() {
        return ingest(DEFAULT_RESOURCE_PATTERN);
    }

    public IngestionResult ingest(String resourcePattern) {
        List<Document> documents = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        try {
            for (Resource resource : resourceResolver.getResources(resourcePattern)) {
                String markdown = resource.getContentAsString(StandardCharsets.UTF_8);
                HuaweiKnowledgeDocument knowledge = parser.parse(markdown);
                if (!isReady(knowledge)) {
                    skipped.add(knowledge.id());
                    continue;
                }
                documents.add(Document.builder()
                        .id(stableVectorDocumentId(knowledge.id()))
                        .text(knowledge.body())
                        .metadata(knowledge.metadata())
                        .build());
            }
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to read Huawei knowledge resources.", ex);
        }
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
        return new IngestionResult(documents.size(), skipped);
    }

    private boolean isReady(HuaweiKnowledgeDocument document) {
        if (!"READY".equalsIgnoreCase(document.status())) {
            return false;
        }
        String normalizedBody = document.body() == null ? "" : document.body().toLowerCase(Locale.ROOT);
        return !normalizedBody.isBlank()
                && !normalizedBody.contains("todo")
                && !normalizedBody.contains("待填写")
                && !normalizedBody.contains("占位");
    }

    private String stableVectorDocumentId(String knowledgeId) {
        String sourceId = knowledgeId == null ? "" : knowledgeId;
        return UUID.nameUUIDFromBytes(("huawei-knowledge:" + sourceId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * Summary of one explicit ingestion run.
     */
    public record IngestionResult(int ingestedCount, List<String> skippedDocumentIds) {
    }
}
