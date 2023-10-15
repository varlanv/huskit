package io.huskit.gradle.containers.plugin;

import io.huskit.log.Log;
import io.huskit.gradle.containers.plugin.internal.ContainersTask;
import io.huskit.gradle.containers.plugin.internal.DockerContainersExtension;
import io.huskit.gradle.containers.plugin.internal.buildservice.ContainersBuildService;
import io.huskit.gradle.common.plugin.model.string.CapitalizedString;
import lombok.RequiredArgsConstructor;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

@RequiredArgsConstructor
public class RegisterContainersTask {

    private final Log log;
    private final ProjectDescription projectDescription;
    private final TaskContainer tasks;
    private final DockerContainersExtension dockerContainersExtension;
    private final Provider<ContainersBuildService> containersBuildServiceProvider;
    private final String dependentTaskName;

    public TaskProvider<ContainersTask> register() {
        String taskName = ContainersTask.NAME + "For" + CapitalizedString.capitalize(dependentTaskName);
        log.info("Registering containers task with name: [{}]", taskName);
        return tasks.register(
                taskName,
                ContainersTask.class,
                containersTask -> {
                    containersTask.getProjectDescription().set(projectDescription);
                    containersTask.getRequestedContainers().addAll(dockerContainersExtension.getContainersRequestedByUser().get());
                    containersTask.getContainers().set(containersBuildServiceProvider);
                    containersTask.usesService(containersBuildServiceProvider);
                });
    }
}
