package com.yali.mactav.modelcore.repository;

import com.yali.mactav.common.enums.ErrorCode;
import com.yali.mactav.common.exception.BusinessException;
import com.yali.mactav.model.enums.ArtifactType;
import com.yali.mactav.model.workspace.NetworkArtifact;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TODO Phase 9: replace this in-memory store with MySQL/Redis backed persistence.
 */
/**
 * Process-local repository for immutable artifact history.
 *
 * <p>It rejects artifactId overwrites and calculates versions from stored
 * history. TODO Phase 9: replace with durable artifact tables/indexes.</p>
 */
public class InMemoryNetworkArtifactRepository {

    private final ConcurrentMap<String, NetworkArtifact> artifacts = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, CopyOnWriteArrayList<String>> artifactIdsByTaskId = new ConcurrentHashMap<>();

    public NetworkArtifact save(NetworkArtifact artifact) {
        NetworkArtifact existing = artifacts.putIfAbsent(artifact.getArtifactId(), artifact);
        if (existing != null) {
            throw new BusinessException(
                    ErrorCode.ARTIFACT_INVALID,
                    "Artifact already exists: " + artifact.getArtifactId());
        }
        artifactIdsByTaskId
                .computeIfAbsent(artifact.getTaskId(), ignored -> new CopyOnWriteArrayList<>())
                .addIfAbsent(artifact.getArtifactId());
        return artifact;
    }

    public Optional<NetworkArtifact> findByArtifactId(String artifactId) {
        return Optional.ofNullable(artifacts.get(artifactId));
    }

    public List<NetworkArtifact> listByTaskId(String taskId) {
        List<String> artifactIds = artifactIdsByTaskId.getOrDefault(taskId, new CopyOnWriteArrayList<>());
        List<NetworkArtifact> result = new ArrayList<>();
        for (String artifactId : artifactIds) {
            NetworkArtifact artifact = artifacts.get(artifactId);
            if (artifact != null) {
                result.add(artifact);
            }
        }
        result.sort(Comparator
                .comparing(NetworkArtifact::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(NetworkArtifact::getVersion, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    public List<NetworkArtifact> listByTaskIdAndType(String taskId, ArtifactType artifactType) {
        return listByTaskId(taskId).stream()
                .filter(artifact -> artifactType == artifact.getArtifactType())
                .toList();
    }

    public int nextVersion(String taskId, ArtifactType artifactType) {
        return listByTaskIdAndType(taskId, artifactType).stream()
                .map(NetworkArtifact::getVersion)
                .filter(version -> version != null && version > 0)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }
}
