package com.yali.mactav.agent.core.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 保存一次 Agent 调用的上下文，比如 taskId、rawText，还有一个扩展用的 attributes Map。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    private String taskId;
    private String rawText;
    private Map<String, Object> attributes = new HashMap<>();

    public AgentContext(String taskId, String rawText) {
        this.taskId = taskId;
        this.rawText = rawText;
    }

    public static AgentContext of(String taskId, String rawText) {
        return new AgentContext(taskId, rawText);
    }
}
