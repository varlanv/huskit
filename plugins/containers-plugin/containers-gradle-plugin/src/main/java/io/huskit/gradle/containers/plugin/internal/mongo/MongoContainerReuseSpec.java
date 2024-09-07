package io.huskit.gradle.containers.plugin.internal.mongo;

import io.huskit.gradle.containers.plugin.api.CleanupSpecView;
import io.huskit.gradle.containers.plugin.api.mongo.MongoContainerReuseSpecView;
import io.huskit.gradle.containers.plugin.internal.CleanupSpec;
import io.huskit.gradle.containers.plugin.internal.ContainerReuseSpec;
import org.gradle.api.Action;
import org.gradle.api.provider.Property;

public interface MongoContainerReuseSpec extends MongoContainerReuseSpecView, ContainerReuseSpec {

    Property<CleanupSpec> getCleanupSpec();

    Property<Boolean> getNewDatabaseForEachTask();

    @Override
    default void cleanup(Action<CleanupSpecView> action) {
        action.execute(getCleanupSpec().get());
    }

    @Override
    default void newDatabaseForEachTask(boolean newDatabaseForEachTask) {
        getNewDatabaseForEachTask().set(newDatabaseForEachTask);
    }
}
