package io.huskit.containers.model;

import io.huskit.containers.model.request.RequestedContainer;
import io.huskit.containers.model.request.RequestedContainers;
import io.huskit.containers.model.started.StartedContainer;
import io.huskit.containers.model.started.StartedContainers;
import io.huskit.containers.model.started.StartedContainersInternal;
import io.huskit.log.Log;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DockerContainers implements Containers {

    private final Log log;
    private final String source;
    private final StartedContainersInternal startedContainersInternal;
    private final RequestedContainers requestedContainers;

    @Override
    public StartedContainers start() {
        log.info("Requesting containers to start");
        return () -> {
            List<RequestedContainer> requestedContainerList = requestedContainers.list();
            if (requestedContainerList.isEmpty()) {
                log.info("Requested container list is empty, so no containers will be started");
                return List.of();
            } else if (requestedContainerList.size() == 1) {
                log.info("Requested container list has only one container, so it will be started synchronously in the current thread");
                return List.of(startedContainersInternal.startOrCreateAndStart(source, requestedContainerList.get(0)));
            } else {
                log.info("Requested container list has [{}] containers, so they will be started asynchronously in separate threads", requestedContainerList.size());
                ExecutorService executorService = Executors.newFixedThreadPool(requestedContainerList.size());
                List<Future<StartedContainer>> startedContainerFutureList = new ArrayList<>(requestedContainerList.size());
                for (RequestedContainer requestedContainer : requestedContainerList) {
                    startedContainerFutureList.add(executorService.submit(() ->
                            startedContainersInternal.startOrCreateAndStart(source, requestedContainer)));
                }
                return startedContainerFutureList.stream().map(future -> {
                    try {
                        return future.get(30, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
            }
        };
    }
}
