package com.yali.mactav.model.agent;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Describes the logical input or output payload expected by a remote agent.
 *
 * <p>This contract is intentionally schema-adjacent metadata, not a parser,
 * validator, or transport implementation.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContract {

    private String contractName;

    private String payloadType;

    private String schemaVersion;

    @Builder.Default
    private List<String> requiredFields = new ArrayList<>();

    private String description;
}
