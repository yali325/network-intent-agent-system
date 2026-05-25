package com.yali.mactav.model.config;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RollbackBlock {

    private String deviceId;

    private String blockId;

    @Builder.Default
    private List<String> commands = new ArrayList<>();

    private Integer order;
}
