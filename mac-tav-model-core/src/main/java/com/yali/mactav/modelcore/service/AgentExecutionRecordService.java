package com.yali.mactav.modelcore.service;

import com.yali.mactav.model.workspace.AgentExecutionRecord;
import java.util.List;

/**
 * Stores structured execution records for agent, tool, A2A, and execution steps.
 *
 * <p>The service records summaries only and must not invoke agents or external
 * models.</p>
 */
public interface AgentExecutionRecordService {

    AgentExecutionRecord appendRecord(String taskId, AgentExecutionRecord record);

    List<AgentExecutionRecord> listRecords(String taskId);
}
