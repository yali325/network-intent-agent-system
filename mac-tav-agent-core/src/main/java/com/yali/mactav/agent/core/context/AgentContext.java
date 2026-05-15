package com.yali.mactav.agent.core.context;

import java.util.HashMap;
import java.util.Map;

public class AgentContext {

    private String taskId;
    private String rawText;
    private Map<String, Object> attributes = new HashMap<>();

    public AgentContext() {
    }

    public AgentContext(String taskId, String rawText) {
        this.taskId = taskId;
        this.rawText = rawText;
    }

    public static AgentContext of(String taskId, String rawText) {
        return new AgentContext(taskId, rawText);
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes == null ? new HashMap<>() : attributes;
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }
}
