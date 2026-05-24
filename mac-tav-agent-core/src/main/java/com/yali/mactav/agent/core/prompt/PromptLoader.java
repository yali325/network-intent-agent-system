package com.yali.mactav.agent.core.prompt;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class PromptLoader {

    private static final String CLASSPATH_PREFIX = "classpath:";

    private PromptLoader() {
    }

    public static String load(String path) {
        String resourcePath = normalize(path);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = classLoader == null ? null : classLoader.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            inputStream = PromptLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "Prompt instruction not found: " + path);
        }
        try (InputStream stream = inputStream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "Prompt instruction load failed: " + path, ex);
        }
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Prompt instruction path must not be blank");
        }
        String normalized = path.trim();
        if (normalized.startsWith(CLASSPATH_PREFIX)) {
            normalized = normalized.substring(CLASSPATH_PREFIX.length());
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }
}
