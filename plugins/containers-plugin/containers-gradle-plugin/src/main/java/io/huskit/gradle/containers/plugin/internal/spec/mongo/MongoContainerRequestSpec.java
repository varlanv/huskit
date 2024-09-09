package io.huskit.gradle.containers.plugin.internal.spec.mongo;

import io.huskit.containers.model.Constants;
import io.huskit.containers.model.ContainerType;
import io.huskit.containers.model.image.DefaultContainerImage;
import io.huskit.containers.model.request.DefaultMongoRequestedContainer;
import io.huskit.containers.model.request.MongoExposedEnvironment;
import io.huskit.containers.model.request.MongoRequestedContainer;
import io.huskit.containers.model.reuse.ContainerCleanupOptions;
import io.huskit.containers.model.reuse.DefaultMongoContainerReuseOptions;
import io.huskit.gradle.containers.plugin.api.mongo.MongoContainerRequestSpecView;
import io.huskit.gradle.containers.plugin.api.mongo.MongoContainerReuseSpecView;
import io.huskit.gradle.containers.plugin.api.mongo.MongoExposedEnvironmentSpecView;
import io.huskit.gradle.containers.plugin.internal.HuskitContainersExtension;
import io.huskit.gradle.containers.plugin.internal.spec.CleanupSpec;
import io.huskit.gradle.containers.plugin.internal.spec.ContainerPortSpec;
import io.huskit.gradle.containers.plugin.internal.spec.ContainerRequestSpec;
import io.huskit.gradle.containers.plugin.internal.spec.FixedContainerPortSpec;
import org.gradle.api.Action;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface MongoContainerRequestSpec extends ContainerRequestSpec, MongoContainerRequestSpecView {

    Property<String> getDatabaseName();

    Property<MongoContainerReuseSpec> getReuse();

    Property<MongoExposedEnvironmentSpec> getExposedEnvironment();

    @Override
    @NotNull
    default Map<String, Object> idProps() {
        var reuse = getReuse().get();
        var exposedEnv = getExposedEnvironment().get();
        var reuseEnabled = reuse.getEnabled().get();
        return Map.of(
                "rootProjectName", getRootProjectName().get(),
                "projectName", reuseEnabled ? "" : getProjectName().get(),
                "image", getImage().get(),
                "databaseName", getDatabaseName().get(),
                "reuseBetweenBuilds", reuse.getReuseBetweenBuilds().get(),
                "newDatabaseForEachTask", reuse.getNewDatabaseForEachTask().get(),
                "reuseEnabled", reuseEnabled,
                "exposedPort", exposedEnv.getPort().get(),
                "exposedConnectionString", exposedEnv.getConnectionString().get(),
                "exposedDatabaseName", exposedEnv.getDatabaseName().get()
        );
    }

    @Override
    default MongoRequestedContainer toRequestedContainer() {
        var containerReuseSpec = getReuse().get();
        var exposedEnvironmentSpec = getExposedEnvironment().get();
        var cleanupSpec = containerReuseSpec.getCleanupSpec().get();
        var port = getPort().get();
        return new DefaultMongoRequestedContainer(
                () -> getProjectPath().get(),
                new MongoExposedEnvironment.Default(
                        exposedEnvironmentSpec.getConnectionString().get(),
                        exposedEnvironmentSpec.getDatabaseName().get(),
                        exposedEnvironmentSpec.getPort().get()
                ),
                getDatabaseName().get(),
                new DefaultContainerImage(getImage().get()),
                port.resolve(port),
                id(),
                new DefaultMongoContainerReuseOptions(
                        Boolean.TRUE.equals(containerReuseSpec.getEnabled().getOrNull()),
                        Boolean.TRUE.equals(containerReuseSpec.getNewDatabaseForEachTask().getOrNull()),
                        Boolean.TRUE.equals(containerReuseSpec.getReuseBetweenBuilds().getOrNull()),
                        ContainerCleanupOptions.after(cleanupSpec.getCleanupAfter().getOrNull())
                )
        );
    }

    default void reuse(Action<MongoContainerReuseSpecView> action) {
        var reuse = getReuse().get();
        reuse.getEnabled().set(true);
        action.execute(reuse);
    }

    default void exposedEnvironment(Action<MongoExposedEnvironmentSpecView> action) {
        var exposedEnvironment = getExposedEnvironment().get();
        action.execute(exposedEnvironment);
    }

    @Override
    default ContainerType containerType() {
        return ContainerType.MONGO;
    }

    @Override
    default void databaseName(String databaseName) {
        getDatabaseName().set(databaseName);
    }

    default void configure(HuskitContainersExtension extension, Action<MongoContainerRequestSpecView> action) {
        var objects = extension.getObjects();
        var cleanupSpec = objects.newInstance(CleanupSpec.class);
        cleanupSpec.getCleanupAfter().convention(Constants.Cleanup.DEFAULT_CLEANUP_AFTER);
        var reuse = objects.newInstance(MongoContainerReuseSpec.class);
        reuse.getEnabled().convention(false);
        reuse.getNewDatabaseForEachTask().convention(false);
        reuse.getReuseBetweenBuilds().convention(false);
        reuse.getCleanupSpec().convention(cleanupSpec);
        var exposedEnvironment = objects.newInstance(MongoExposedEnvironmentSpec.class);
        exposedEnvironment.getConnectionString().convention(Constants.Mongo.DEFAULT_CONNECTION_STRING_ENV);
        exposedEnvironment.getDatabaseName().convention(Constants.Mongo.DEFAULT_DB_NAME_ENV);
        exposedEnvironment.getPort().convention(Constants.Mongo.DEFAULT_PORT_ENV);
        var port = objects.newInstance(ContainerPortSpec.class);
        var fixedPort = objects.newInstance(FixedContainerPortSpec.class);
        port.getFixed().convention(fixedPort);
        port.getDynamic().convention(true);
        port.getContainerDefaultPort().set(Constants.Mongo.DEFAULT_PORT);
        getReuse().convention(reuse);
        getDatabaseName().convention(Constants.Mongo.DEFAULT_DB_NAME);
        getRootProjectName().convention(extension.getRootProjectName());
        getProjectPath().convention(extension.getProjectPath());
        getProjectName().convention(extension.getProjectName());
        getImage().convention(Constants.Mongo.DEFAULT_IMAGE);
        getExposedEnvironment().convention(exposedEnvironment);
        getPort().convention(port);
        action.execute(this);
    }
}