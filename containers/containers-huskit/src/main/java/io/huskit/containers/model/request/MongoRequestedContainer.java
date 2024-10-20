package io.huskit.containers.model.request;

import io.huskit.containers.model.ContainerType;
import io.huskit.containers.model.reuse.MongoContainerReuseOptions;

public interface MongoRequestedContainer extends RequestedContainer {

    @Override
    MongoContainerReuseOptions reuseOptions();

    MongoExposedEnvironment exposedEnvironment();

    String databaseName();

    @Override
    default ContainerType containerType() {
        return ContainerType.MONGO;
    }
}
