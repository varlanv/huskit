package io.huskit.gradle.containers.plugin.internal;

import io.huskit.containers.model.ContainerType;
import io.huskit.containers.model.Containers;
import io.huskit.containers.model.DockerContainers;
import io.huskit.containers.model.request.MongoRequestedContainer;
import io.huskit.containers.model.request.RequestedContainers;
import io.huskit.containers.model.started.StartedContainer;
import io.huskit.containers.model.started.StartedContainersInternal;
import io.huskit.containers.testcontainers.mongo.MongoContainer;
import io.huskit.gradle.containers.plugin.ProjectDescription;
import io.huskit.log.Log;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class ContainersApplication {

    private final Log log;
    private volatile StartedContainersInternal startedContainersInternal;

    public Containers containers(ProjectDescription projectDescription, RequestedContainers requestedContainers, Log log) {
        return new ValidatedDockerContainers(
                new DockerContainers(
                        log,
                        projectDescription.path(),
                        getDockerStartedContainersInternal(),
                        requestedContainers
                ),
                requestedContainers
        );
    }

    public void stop() throws Exception {
        List<StartedContainer> startedContainers = getDockerStartedContainersInternal().list();
        if (!startedContainers.isEmpty()) {
            long timeMillis = System.currentTimeMillis();
            if (startedContainers.size() == 1) {
                tryStop(startedContainers.get(0));
                log.lifecycle("Stopped single container in [{}] ms", System.currentTimeMillis() - timeMillis);
            } else {
                CountDownLatch countDownLatch = new CountDownLatch(startedContainers.size());
                ExecutorService executorService = Executors.newFixedThreadPool(startedContainers.size());
                for (StartedContainer startedContainer : startedContainers) {
                    executorService.execute(() -> {
                        try {
                            tryStop(startedContainer);
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                }
                countDownLatch.await(5, TimeUnit.SECONDS);
                log.lifecycle("Stopped [{}] containers in [{}] ms", startedContainers.size(), System.currentTimeMillis() - timeMillis);
            }
        }
    }

    private DockerStartedContainersInternal getDockerStartedContainersInternal() {
        if (startedContainersInternal == null) {
            synchronized (this) {
                if (startedContainersInternal == null) {
                    log.info("startedContainersInternal is not created, entering synchronized block to create instance");
                    startedContainersInternal = new DockerStartedContainersInternal(
                            log,
                            new KnownDockerContainers(
                                    log,
                                    Map.of(
                                            ContainerType.MONGO, requestedContainer -> new MongoContainer(
                                                    log,
                                                    (MongoRequestedContainer) requestedContainer
                                            )
                                    )
                            )
                    );
                }
            }
        }
        return (DockerStartedContainersInternal) startedContainersInternal;
    }

    private void tryStop(StartedContainer startedContainer) {
        try {
            startedContainer.close();
        } catch (Exception exception) {
            log.error("Failed to stop container [{}]. Ignoring exception", startedContainer.id(), exception);
        }
    }
}
