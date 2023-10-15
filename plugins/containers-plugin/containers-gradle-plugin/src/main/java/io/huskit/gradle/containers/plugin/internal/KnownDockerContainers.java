package io.huskit.gradle.containers.plugin.internal;

import io.huskit.containers.model.ContainerType;
import io.huskit.log.Log;
import io.huskit.containers.model.exception.UnknownContainerTypeException;
import io.huskit.containers.model.request.RequestedContainer;
import io.huskit.containers.model.started.StartedContainerInternal;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
public class KnownDockerContainers {

    private final Log log;
    private final Map<ContainerType, Function<RequestedContainer, StartedContainerInternal>> knownContainers;

    public StartedContainerInternal start(RequestedContainer requestedContainer) {
        ContainerType containerType = requestedContainer.containerType();
        return Optional.ofNullable(knownContainers.get(containerType))
                .map(fn -> {
                    log.info("Starting container of type [{}]", containerType);
                    return fn.apply(requestedContainer);
                })
                .orElseThrow(() -> new UnknownContainerTypeException(containerType));
    }
}
