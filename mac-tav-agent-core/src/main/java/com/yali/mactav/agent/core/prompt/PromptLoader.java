package com.yali.mactav.agent.core.prompt;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Classpath prompt loader for agent instruction markdown files.
 *
 * <p>Prompt content belongs in resources, not Java constants. This loader is
 * generic agent-core infrastructure and does not own business prompt text.</p>
 */
public class PromptLoader {

    public String loadFromClasspath(String path) {
        if (path == null || path.isBlank()) {
            throw new BusinessException(ErrorCode.PROMPT_NOT_FOUND, "Prompt path must not be blank");
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = PromptLoader.class.getClassLoader();
        }
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new BusinessException(ErrorCode.PROMPT_NOT_FOUND, "Prompt resource not found: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new BusinessException(ErrorCode.PROMPT_LOAD_FAILED, "Failed to load prompt resource: " + path, ex);
        }
    }
}
