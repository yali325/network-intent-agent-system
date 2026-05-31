package com.yali.mactav.configuration.knowledge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses Markdown knowledge documents with simple YAML front matter.
 */
public class HuaweiKnowledgeMarkdownParser {

    public HuaweiKnowledgeDocument parse(String markdown) {
        if (markdown == null || !markdown.startsWith("---")) {
            throw new IllegalArgumentException("Knowledge markdown must start with front matter.");
        }
        int end = markdown.indexOf("\n---", 3);
        if (end < 0) {
            throw new IllegalArgumentException("Knowledge markdown front matter is not closed.");
        }
        String frontMatter = markdown.substring(3, end).trim();
        String body = markdown.substring(end + 4).trim();
        Map<String, Object> metadata = parseFrontMatter(frontMatter);

        return new HuaweiKnowledgeDocument(
                stringValue(metadata.get("id")),
                stringValue(metadata.get("title")),
                stringValue(metadata.get("vendor")),
                stringValue(metadata.get("platform")),
                stringValue(metadata.get("feature")),
                listValue(metadata.get("tags")),
                stringValue(metadata.get("knowledgeType")),
                stringValue(metadata.get("generationSourceType")),
                stringValue(metadata.get("riskLevel")),
                stringValue(metadata.get("status")),
                stringValue(metadata.get("version")),
                stringValue(metadata.get("updatedAt")),
                body,
                metadata);
    }

    private Map<String, Object> parseFrontMatter(String frontMatter) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (String rawLine : frontMatter.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            metadata.put(key, parseValue(value));
        }
        return metadata;
    }

    private Object parseValue(String value) {
        if (value.startsWith("[") && value.endsWith("]")) {
            String inner = value.substring(1, value.length() - 1).trim();
            if (inner.isEmpty()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (String item : inner.split(",")) {
                values.add(unquote(item.trim()));
            }
            return values;
        }
        return unquote(value);
    }

    private List<String> listValue(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value == null || stringValue(value).isBlank()) {
            return List.of();
        }
        return List.of(stringValue(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
