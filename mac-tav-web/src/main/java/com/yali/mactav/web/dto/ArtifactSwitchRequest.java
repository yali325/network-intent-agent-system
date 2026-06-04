package com.yali.mactav.web.dto;

/**
 * Request body for switching a workspace current artifact pointer.
 */
public class ArtifactSwitchRequest {

    private String artifactType;

    private String reason;

    private String actor;

    public String getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(String artifactType) {
        this.artifactType = artifactType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
}
