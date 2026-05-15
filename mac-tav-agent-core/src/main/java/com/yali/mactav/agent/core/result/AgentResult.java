package com.yali.mactav.agent.core.result;

public class AgentResult<O> {

    private boolean success;
    private O data;
    private String message;
    private String errorCode;
    private String agentName;
    private String stage;

    public AgentResult() {
    }

    public AgentResult(boolean success, O data, String message, String errorCode, String agentName, String stage) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.errorCode = errorCode;
        this.agentName = agentName;
        this.stage = stage;
    }

    public static <O> AgentResult<O> success(O data, String message, String agentName, String stage) {
        return new AgentResult<>(true, data, message, null, agentName, stage);
    }

    public static <O> AgentResult<O> failure(String message, String errorCode, String agentName, String stage) {
        return new AgentResult<>(false, null, message, errorCode, agentName, stage);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public O getData() {
        return data;
    }

    public void setData(O data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }
}
