package com.yali.mactav.configuration.knowledge;

import java.util.List;
import java.util.Map;

/**
 * Parsed Huawei command knowledge document with front matter metadata.
 */
public record HuaweiKnowledgeDocument(
        String id,
        String title,
        String vendor,
        String platform,
        String feature,
        List<String> tags,
        String knowledgeType,
        String generationSourceType,
        String riskLevel,
        String status,
        String version,
        String updatedAt,
        String body,
        Map<String, Object> metadata) {
}
