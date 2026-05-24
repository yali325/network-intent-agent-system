package com.yali.mactav.agent.core.llm;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class LlmPromptRunner {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public LlmPromptRunner(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    public String run(String systemPrompt, String userPrompt) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new BusinessException(
                    ErrorCode.MODEL_CALL_FAILED,
                    "LLM ChatClient is not available; configure a chat model provider in the application module"
            );
        }

        String content = builder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.MODEL_CALL_FAILED, "LLM response content is empty");
        }
        return content;
    }
}
