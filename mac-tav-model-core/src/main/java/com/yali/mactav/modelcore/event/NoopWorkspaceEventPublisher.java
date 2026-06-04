package com.yali.mactav.modelcore.event;

import com.yali.mactav.model.workspace.WorkspaceEvent;

/**
 * Test or explicit opt-in publisher that intentionally skips realtime delivery.
 */
public class NoopWorkspaceEventPublisher implements WorkspaceEventPublisher {

    @Override
    public void publish(WorkspaceEvent event) {
        // Explicit no-op for test/inmemory/noop publisher modes.
    }
}
