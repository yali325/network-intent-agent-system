package com.yali.mactav.web.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateDemoTaskRequest {

    @NotBlank(message = "rawText must not be blank")
    private String rawText;

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }
}
