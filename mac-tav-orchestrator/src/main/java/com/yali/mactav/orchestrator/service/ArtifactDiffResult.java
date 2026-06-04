package com.yali.mactav.orchestrator.service;

import com.yali.mactav.model.workspace.NetworkArtifact;

/**
 * Read-side artifact diff result containing two persisted artifact versions.
 */
public record ArtifactDiffResult(NetworkArtifact from, NetworkArtifact to) {
}
