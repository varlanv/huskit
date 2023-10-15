package io.huskit.gradle.containers.plugin.internal;

import io.huskit.containers.model.MongoStartedContainer;
import io.huskit.containers.model.started.StartedContainer;
import io.huskit.gradle.containers.plugin.ProjectDescription;
import io.huskit.gradle.containers.plugin.api.ContainerRequestedByUser;
import io.huskit.gradle.containers.plugin.internal.buildservice.ContainersBuildService;
import io.huskit.log.Log;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.testing.Test;

import java.util.List;

@RequiredArgsConstructor
public class AddContainersEnvironment implements Action<Task> {

    private final Log log;
    private final ProjectDescription projectDescription;
    private final Provider<ContainersBuildService> containersBuildServiceProvider;
    private final ListProperty<ContainerRequestedByUser> containersRequestedByUser;
    private final String connectionStringEnvironmentVariableName = "MONGO_CONNECTION_STRING";
    private final String dbNameEnvironmentVariable = "MONGO_CONNECTION_STRING";

    private final String portEnvironmentVariableName = "MONGO_PORT";

    @Override
    public void execute(Task task) {
        if (task instanceof Test) {
            Test test = (Test) task;
            ContainersBuildService containersBuildService = containersBuildServiceProvider.get();
            List<StartedContainer> startedContainers = containersBuildService.containers(
                    projectDescription,
                    new RequestedContainersFromGradleUser(
                            log,
                            containersRequestedByUser.get()
                    ),
                    log
            ).start().list();
            MongoStartedContainer startedContainer = (MongoStartedContainer) startedContainers.stream().findFirst().get();
            test.setEnvironment(startedContainer.environment());
        } else {
            log.info("Task [{}] is not a test task, so environment variables will not be added", task.getName());
        }
    }
}
