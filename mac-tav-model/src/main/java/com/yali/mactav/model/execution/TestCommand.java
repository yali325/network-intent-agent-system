package com.yali.mactav.model.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCommand {

    private String testId;
    private String type;
    private String source;
    private String target;
    private String expected;
    private String command;
}
