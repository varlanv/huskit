package io.huskit.gradle.containers.plugin.internal;

import io.huskit.containers.model.ContainerType;
import io.huskit.containers.model.DefaultRequestedContainer;
import io.huskit.log.Log;
import io.huskit.containers.model.image.DefaultContainerImage;
import io.huskit.containers.model.port.FixedContainerPort;
import io.huskit.containers.model.request.DefaultMongoRequestedContainer;
import io.huskit.containers.model.request.RequestedContainer;
import io.huskit.containers.model.request.RequestedContainers;
import io.huskit.containers.model.reuse.DefaultMongoContainerReuse;
import io.huskit.gradle.containers.plugin.api.ContainerRequestedByUser;
import io.huskit.gradle.containers.plugin.api.ContainerRequestedByUserForTask;
import io.huskit.gradle.containers.plugin.api.MongoContainerRequestedByUser;
import io.huskit.gradle.containers.plugin.api.MongoContainerReuseRequestedByUser;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RequestedContainersFromGradleUser implements RequestedContainers {

    private final Log log;
    private final Collection<ContainerRequestedByUser> containersRequestedByUser;

    @Override
    public List<RequestedContainer> list() {
        return containersRequestedByUser.stream()
                .map(requested -> (ContainerRequestedByUserForTask) requested)
                .map(requested -> {
                    ContainerType containerType = requested.containerType();
                    log.info("Preparing container request with type [{}], id [{}]", containerType, requested.id());
                    if (containerType == ContainerType.MONGO) {
                        MongoContainerRequestedByUser mongoRequested = (MongoContainerRequestedByUser) requested;
                        MongoContainerReuseRequestedByUser containerReuseSpec = mongoRequested.getReuse().getOrNull();
                        return new DefaultMongoRequestedContainer(
                                new DefaultRequestedContainer(
                                        () -> requested.getProjectPath().get(),
                                        new DefaultContainerImage(requested.getImage().get()),
                                        requested.id(),
                                        containerType,
                                        Optional.ofNullable(requested.getFixedPort().getOrNull()).map(FixedContainerPort::new).orElse(null),
                                        new DefaultMongoContainerReuse(
                                                containerReuseSpec != null && Boolean.TRUE.equals(containerReuseSpec.getAllowed().getOrNull()),
                                                containerReuseSpec != null && Boolean.TRUE.equals(containerReuseSpec.getNewDatabaseForEachTask().getOrNull()),
                                                containerReuseSpec != null && Boolean.TRUE.equals(containerReuseSpec.getReuseBetweenBuilds().getOrNull())
                                        )
                                ),
                                mongoRequested.getDatabaseName().get()
                        );
                    } else {
                        throw new UnsupportedOperationException("Container type [" + containerType + "] is not supported");
                    }
                })
                .collect(Collectors.toList());
    }
}
