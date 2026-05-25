package com.yali.mactav.modelcore;

import com.yali.mactav.modelcore.artifact.ArtifactPayloadSerializer;
import com.yali.mactav.modelcore.artifact.NetworkArtifactFactory;
import com.yali.mactav.modelcore.repository.InMemoryNetworkArtifactRepository;
import com.yali.mactav.modelcore.repository.InMemoryNetworkWorkspaceRepository;
import com.yali.mactav.modelcore.repository.InMemoryWorkspaceEventRepository;
import com.yali.mactav.modelcore.service.InMemoryNetworkArtifactService;
import com.yali.mactav.modelcore.service.InMemoryNetworkWorkspaceService;
import com.yali.mactav.modelcore.service.InMemoryWorkspaceEventService;
import com.yali.mactav.modelcore.service.NetworkArtifactService;
import com.yali.mactav.modelcore.service.NetworkWorkspaceService;
import com.yali.mactav.modelcore.service.WorkspaceEventService;
import com.yali.mactav.modelcore.validator.ArtifactValidator;
import com.yali.mactav.modelcore.validator.WorkspaceStateValidator;

public final class ModelCoreTestFixture {

    private ModelCoreTestFixture() {
    }

    public static Services services() {
        WorkspaceStateValidator workspaceStateValidator = new WorkspaceStateValidator();
        ArtifactValidator artifactValidator = new ArtifactValidator();
        ArtifactPayloadSerializer serializer = new ArtifactPayloadSerializer();
        NetworkArtifactFactory artifactFactory = new NetworkArtifactFactory(serializer);
        InMemoryNetworkArtifactRepository artifactRepository = new InMemoryNetworkArtifactRepository();
        InMemoryNetworkWorkspaceRepository workspaceRepository = new InMemoryNetworkWorkspaceRepository();
        WorkspaceEventService eventService = new InMemoryWorkspaceEventService(
                new InMemoryWorkspaceEventRepository(),
                workspaceStateValidator);
        NetworkArtifactService artifactService = new InMemoryNetworkArtifactService(
                artifactRepository,
                artifactFactory,
                artifactValidator,
                workspaceStateValidator);
        NetworkWorkspaceService workspaceService = new InMemoryNetworkWorkspaceService(
                workspaceRepository,
                artifactService,
                eventService,
                workspaceStateValidator,
                artifactValidator);
        return new Services(workspaceService, artifactService, eventService);
    }

    public record Services(
            NetworkWorkspaceService workspaceService,
            NetworkArtifactService artifactService,
            WorkspaceEventService eventService) {
    }
}
