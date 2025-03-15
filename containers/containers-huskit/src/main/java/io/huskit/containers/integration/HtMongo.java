package io.huskit.containers.integration;

import io.huskit.common.HtConstants;
import io.huskit.common.Log;
import io.huskit.common.Mutable;
import io.huskit.common.function.MemoizedSupplier;
import io.huskit.common.port.DynamicContainerPort;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtContainers;
import io.huskit.containers.api.docker.HtDocker;
import io.huskit.containers.api.image.HtImgName;
import io.huskit.containers.model.ContainerType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public final class HtMongo implements HtServiceContainer {

    HtImgName imageName;
    DefContainerSpec containerSpec;
    Boolean newDatabaseForEachRequest;
    String databaseName;
    DefDockerClientSpec dockerClientSpec;
    AtomicInteger databaseNameCounter;
    Mutable<Log> log;

    public static HtMongo fromImage(CharSequence image) {
        var imageName = HtImgName.of(image);
        var log = Mutable.of(Log.noop());
        var defContainerSpec = new DefContainerSpec(log, imageName, ContainerType.MONGO)
            .addProperty(HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV, HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV)
            .addProperty(HtConstants.Mongo.DEFAULT_DB_NAME_ENV, HtConstants.Mongo.DEFAULT_DB_NAME_ENV)
            .addProperty(HtConstants.Mongo.DEFAULT_PORT_ENV, HtConstants.Mongo.DEFAULT_PORT_ENV)
            .await().forLogMessageContaining("Waiting for connections");
        var newDbEachReq = false;
        var dbName = HtConstants.Mongo.DEFAULT_DB_NAME;
        var dockerClientSpec = new DefDockerClientSpec();
        var dbNameCounter = new AtomicInteger();
        var htMongo = new HtMongo(
            imageName,
            defContainerSpec,
            newDbEachReq,
            dbName,
            dockerClientSpec,
            dbNameCounter,
            log
        );
        dockerClientSpec.setParent(htMongo);
        return htMongo;
    }

    @Override
    public HtMongo withDockerClientSpec(Consumer<DockerClientSpec> dockerClientSpecAction) {
        dockerClientSpecAction.accept(dockerClientSpec);
        return this;
    }

    @Override
    public HtMongo withContainerSpec(Consumer<ContainerSpec> containerSpecAction) {
        containerSpecAction.accept(containerSpec);
        return this;
    }

    public HtMongo withContainerSpec(DefContainerSpec defContainerSpec) {
        return new HtMongo(
            imageName,
            defContainerSpec,
            newDatabaseForEachRequest,
            databaseName,
            dockerClientSpec,
            databaseNameCounter,
            log
        );
    }

    @Override
    public HtMongo withLogger(Log log) {
        this.log.set(log);
        return this;
    }

    public HtMongo withNewDatabaseForEachRequest(Boolean newDatabaseForEachRequest) {
        return new HtMongo(
            imageName,
            containerSpec,
            newDatabaseForEachRequest,
            databaseName,
            dockerClientSpec,
            databaseNameCounter,
            log
        );
    }

    @Override
    public MongoStartedContainer start() {
        var reuseEnabled = containerSpec.reuseSpec().value().check(ReuseWithTimeout::enabled);
        var htDocker = dockerClientSpec
            .docker()
            .orElseGet(
                () -> {
                    var docker = HtDocker.anyClient();
                    if (!reuseEnabled) {
                        // todo instead of disabling cleanup on close, add it to specific containers
                        return docker.withCleanOnClose(true);
                    }
                    return docker;
                }
            );
        var container = findExisting(
            htDocker.containers(),
            reuseEnabled
        ).orElseGet(
            () -> {
                containerSpec.labels().pair(HtConstants.CONTAINER_STARTED_AT_LABEL, System.currentTimeMillis());
                var containers = htDocker.containers();
                var port = containerSpec.ports()
                    .port()
                    .mapOr(
                        p -> Map.entry(p.hostValue(), p.containerValue().orElseThrow()),
                        () -> Map.entry(new DynamicContainerPort().hostValue(), HtConstants.Mongo.DEFAULT_PORT)
                    );
                debug(() -> "Starting new container with image: [%s]".formatted(imageName.reference()));
                return containers
                    .run(
                        imageName.reference(),
                        runSpec -> {
                            var envSpec = containerSpec.envSpec();
                            envSpec.envMap().ifPresent(runSpec::withEnv);
                            var labelSpec = containerSpec.labelSpec();
                            labelSpec.labelMap().ifPresent(runSpec::withLabels);
                            var waitSpec = containerSpec.waitSpec();
                            waitSpec.textWait().ifPresent(waiter -> runSpec.withLookFor(waiter.text(), waiter.duration()));
                            runSpec.withPortBinding(port.getKey(), port.getValue());
                        }
                    )
                    .exec()
                    .block();
            }
        );
        var port = container.firstMappedPort();
        var dbName = Mutable.<String>of();
        Supplier<Map<String, String>> containerPropsMap = MemoizedSupplier.of(
            () -> {
                var connectionString = Mutable.<String>of();
                if (reuseEnabled && newDatabaseForEachRequest) {
                    var counter = databaseNameCounter.incrementAndGet();
                    var db = databaseName + "_" + counter;
                    var conn = HtConstants.Mongo.CONNECTION_STRING_PATTERN.apply("localhost", container.firstMappedPort()) + "/" + db;
                    dbName.set(db);
                    connectionString.set(conn);
                } else {
                    var conn = HtConstants.Mongo.CONNECTION_STRING_PATTERN.apply("localhost", container.firstMappedPort());
                    dbName.set(databaseName);
                    connectionString.set(conn);
                }
                return Map.of(
                    containerSpec.properties().get(HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV), connectionString.require(),
                    containerSpec.properties().get(HtConstants.Mongo.DEFAULT_DB_NAME_ENV), dbName.require(),
                    containerSpec.properties().get(HtConstants.Mongo.DEFAULT_PORT_ENV), String.valueOf(port)
                );
            }
        );
        return new DfMongoStartedContainer(
            container.id(),
            reuseEnabled,
            newDatabaseForEachRequest,
            () -> containerPropsMap.get().get(containerSpec.properties().get(HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV)),
            containerSpec::hash,
            containerPropsMap,
            htDocker
        );
    }

    @Override
    public ContainerType containerType() {
        return ContainerType.MONGO;
    }

    private Optional<HtContainer> findExisting(HtContainers htContainers, Boolean reuseEnabled) {
        if (!reuseEnabled) {
            debug(() -> "Reuse is disabled, skipping existing container lookup");
            return Optional.empty();
        }
        var before = System.currentTimeMillis();
        var reuseWithTimeout = containerSpec.reuseSpec().value().require();
        var hash = containerSpec.hash();
        debug(() -> "Looking for container with hash [%s]".formatted(hash));
        var existingContainers = htContainers
            .list(listSpec -> listSpec.withLabelFilter(HtConstants.CONTAINER_HASH_LABEL, hash))
            .asList()
            .block();
        if (existingContainers.size() == 1) {
            debug(
                () -> "Found container with hash: [%s], lookup took %s"
                    .formatted(hash, Duration.ofMillis(System.currentTimeMillis() - before))
            );
            var container = existingContainers.get(0);
            var now = Instant.now();
            var cleanupAfterTime = container.createdAt().plus(reuseWithTimeout.cleanupAfter());
            if (now.isAfter(cleanupAfterTime)) {
                debug(() -> "Container with hash: [%s] is expired, removing".formatted(hash));
                htContainers.remove(container.id(), spec -> spec.withForce().withVolumes()).exec();
                return Optional.empty();
            } else {
                return Optional.of(container);
            }
        } else if (existingContainers.size() > 1) {
            var idsForRemoval = existingContainers.stream()
                .map(HtContainer::id)
                .collect(Collectors.toSet());
            debug(() -> "Find more than one container with hash [%s], all containers will be removed - %s".formatted(hash, idsForRemoval));
            htContainers.remove(
                idsForRemoval,
                spec -> spec.withForce().withVolumes()
            );
            return Optional.empty();
        } else {
            containerSpec.labels()
                .map(
                    Map.of(
                        HtConstants.CONTAINER_HASH_LABEL, hash,
                        HtConstants.CONTAINER_CLEANUP_AFTER_LABEL, reuseWithTimeout.cleanupAfter().toSeconds()
                    )
                );
            debug(
                () -> "Didn't find container with hash: [%s], lookup took %s"
                    .formatted(hash, Duration.ofMillis(System.currentTimeMillis() - before))
            );
            return Optional.empty();
        }
    }

    private void debug(Supplier<String> message) {
        log.require().debug(() -> HtMongo.class.getName() + " - " + message.get());
    }

    public interface MongoStartedContainer extends HtStartedContainer {

        String connectionString();
    }

    @Getter
    @RequiredArgsConstructor
    private static class DfMongoStartedContainer implements MongoStartedContainer {

        String id;
        Boolean reuseEnabled;
        Boolean newDatabaseForEachRequest;
        Supplier<String> conStr;
        Supplier<String> hashSupplier;
        Supplier<Map<String, String>> props;
        HtDocker htDocker;
        AtomicBoolean stopped = new AtomicBoolean(false);

        @Override
        public Map<String, String> properties() {
            return Collections.unmodifiableMap(props.get());
        }

        @Override
        public String hash() {
            return hashSupplier.get();
        }

        @Override
        public void stopAndRemove() {
            synchronized (this) {
                if (reuseEnabled && newDatabaseForEachRequest) {
                    htDocker.containers().execInContainer(id, "/bin/sh", List.of("-c", HtConstants.Mongo.DROP_COMMAND));
                } else {
                    if (stopped.compareAndSet(false, true)) {
                        htDocker.containers().remove(id, s -> s.withVolumes().withForce()).exec();
                    }
                }
            }
        }

        @Override
        public Boolean isStopped() {
            return stopped.get();
        }

        @Override
        public String connectionString() {
            return conStr.get();
        }
    }
}
