package com.yali.mactav.orchestrator.service;

import com.yali.mactav.model.workspace.NetworkWorkspace;

public interface TaskOrchestratorService {

    NetworkWorkspace runDemoTask(String rawText);
}
