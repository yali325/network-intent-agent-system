package com.yali.mactav.agent.core.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Shared safe logging helpers for MAC-TAV agent hooks.
 *
 * <p>This support class intentionally logs identifiers and state keys only. It
 * must not log raw prompts, API keys, request headers, or full model payloads.</p>
 */
final class HookLogSupport {

    private HookLogSupport() {
    }

    static String safeSummary(String agentName, OverAllState state, RunnableConfig config) {
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add("agentName=" + valueOrUnknown(agentName));
        readContext(config, "taskId").ifPresent(value -> joiner.add("taskId=" + value));
        readContext(config, "traceId").ifPresent(value -> joiner.add("traceId=" + value));
        readContext(config, "stage").ifPresent(value -> joiner.add("stage=" + value));
        if (state != null && state.data() != null && !state.data().isEmpty()) {
            joiner.add("stateKeys=" + state.data().keySet());
        }
        return joiner.toString();
    }

    static Optional<String> readContext(RunnableConfig config, String key) {
        if (config == null || key == null || key.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> context = config.context();
        if (context == null) {
            return Optional.empty();
        }
        Object value = context.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(value));
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
