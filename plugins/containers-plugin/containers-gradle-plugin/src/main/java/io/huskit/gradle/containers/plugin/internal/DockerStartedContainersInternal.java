package io.huskit.gradle.containers.plugin.internal;

import io.huskit.log.Log;
import io.huskit.containers.model.request.RequestedContainer;
import io.huskit.containers.model.started.StartedContainer;
import io.huskit.containers.model.started.StartedContainerInternal;
import io.huskit.containers.model.started.StartedContainersInternal;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public class DockerStartedContainersInternal implements StartedContainersInternal {

    private final Log log;
    private final ConcurrentMap<String, StartedContainerInternal> startedContainersById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, StartedContainerInternal> startedContainersBySourceAndId = new ConcurrentHashMap<>();
    private final List<StartedContainerInternal> allStartedContainers = new CopyOnWriteArrayList<>();
    private final KnownDockerContainers knownDockerContainers;

    @Override
    public List<StartedContainer> list() {
        return new ArrayList<>(allStartedContainers);
    }

    @Override
    public StartedContainer startOrCreateAndStart(String source, RequestedContainer requestedContainer) {
        String key = requestedContainer.id().value();
        log.info("Starting container with key [{}]", key);
        StartedContainerInternal startedContainer = startedContainersById.get(key);
        if (requestedContainer.containerReuse().allowed()) {
            log.info("Container with key [{}] is reusable", key);
            if (startedContainer == null) {
                synchronized (this) {
                    startedContainer = startedContainersById.get(key);
                    if (startedContainer == null) {
                        log.info("Container with key [{}] is not started yet. Sync block is entered", key);
                        startedContainer = knownDockerContainers.start(requestedContainer);
                        startedContainersById.put(key, startedContainer);
                        allStartedContainers.add(startedContainer);
                        startedContainer.start();
                    }
                }
            }
        } else {
            String nonReusableKey = source + key;
            log.info("Container with key [{}] is not reusable", nonReusableKey);
            startedContainer = startedContainersBySourceAndId.get(nonReusableKey);
            if (startedContainer == null) {
                synchronized (this) {
                    startedContainer = startedContainersBySourceAndId.get(nonReusableKey);
                    if (startedContainer == null) {
                        startedContainer = knownDockerContainers.start(requestedContainer);
                        startedContainer.start();
                        allStartedContainers.add(startedContainer);
                        startedContainersBySourceAndId.put(nonReusableKey, startedContainer);
                    }
                }
            }
        }
        return startedContainer;
    }
}
