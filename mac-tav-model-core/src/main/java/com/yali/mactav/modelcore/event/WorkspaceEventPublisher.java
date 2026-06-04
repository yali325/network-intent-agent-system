package com.yali.mactav.modelcore.event;

import com.yali.mactav.model.workspace.WorkspaceEvent;

/**
 * Publishes persisted workspace events to realtime delivery infrastructure.
 */
public interface WorkspaceEventPublisher {

    void publish(WorkspaceEvent event);
}
