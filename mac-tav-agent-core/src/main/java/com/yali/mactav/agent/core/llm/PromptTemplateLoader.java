package com.yali.mactav.agent.core.llm;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class PromptTemplateLoader {

    private final ResourceLoader resourceLoader;

    public PromptTemplateLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String load(String location) {
        Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "Prompt template not found: " + location);
        }
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new BusinessException(
                    ErrorCode.PIPELINE_FAILED,
                    "Prompt template load failed: " + location
            );
        }
    }
}
