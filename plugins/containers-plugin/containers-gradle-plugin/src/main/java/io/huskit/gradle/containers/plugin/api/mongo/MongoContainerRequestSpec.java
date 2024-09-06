package io.huskit.gradle.containers.plugin.api.mongo;

import io.huskit.containers.model.ContainerType;
import io.huskit.containers.model.id.ContainerId;
import io.huskit.gradle.containers.plugin.api.ContainerRequestForTaskSpec;
import io.huskit.gradle.containers.plugin.internal.DefaultContainerId;
import org.gradle.api.Action;
import org.gradle.api.provider.Property;

import java.util.Map;

public interface MongoContainerRequestSpec extends ContainerRequestForTaskSpec {

    Property<String> getDatabaseName();

    Property<MongoContainerReuseSpec> getReuse();

    Property<MongoExposedEnvironmentSpec> getExposedEnvironment();

    default ContainerId id() {
        var reuse = getReuse().get();
        var exposedEnv = getExposedEnvironment().get();
        var reuseEnabled = reuse.getEnabled().get();
        return new DefaultContainerId(this).with(Map.of(
                "projectName", reuseEnabled ? "" : getProjectName().get(),
                "databaseName", getDatabaseName().get(),
                "reuseBetweenBuilds", reuse.getReuseBetweenBuilds().get(),
                "newDatabaseForEachTask", reuse.getNewDatabaseForEachTask().get(),
                "reuseEnabled", reuseEnabled,
                "exportedPort", exposedEnv.getPort().get(),
                "exportedConnectionString", exposedEnv.getConnectionString().get(),
                "exportedDatabaseName", exposedEnv.getDatabaseName().get()
        ));
    }

    default void reuse(Action<MongoContainerReuseSpec> action) {
        var reuse = getReuse().get();
        reuse.getEnabled().set(true);
        action.execute(reuse);
    }

    default void reuse() {
        var reuse = getReuse().get();
        reuse.getEnabled().set(true);
    }

    default void exposedEnvironment(Action<MongoExposedEnvironmentSpec> action) {
        var exposedEnvironment = getExposedEnvironment().get();
        action.execute(exposedEnvironment);
    }

    @Override
    default ContainerType containerType() {
        return ContainerType.MONGO;
    }
}
