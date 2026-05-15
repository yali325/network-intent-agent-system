package com.yali.mactav.agent.core.llm;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class LlmPromptRunner {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final Environment environment;

    public LlmPromptRunner(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                           Environment environment) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.environment = environment;
    }

    public String run(String systemPrompt, String userPrompt) {
        ensureDashScopeApiKeyConfigured();
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new BusinessException(
                    ErrorCode.PIPELINE_FAILED,
                    "LLM ChatClient is not available; set spring.ai.model.chat=dashscope when mactav.agent.intent-mode=llm"
            );
        }

        String content = builder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.PIPELINE_FAILED, "LLM response content is empty");
        }
        return content;
    }

    private void ensureDashScopeApiKeyConfigured() {
        String commonApiKey = environment.getProperty("spring.ai.dashscope.api-key");
        String chatApiKey = environment.getProperty("spring.ai.dashscope.chat.api-key");
        if (isBlank(commonApiKey) && isBlank(chatApiKey)) {
            throw new BusinessException(
                    ErrorCode.PIPELINE_FAILED,
                    "DashScope API Key is required for llm intent mode; configure environment variable aliApi-key"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
