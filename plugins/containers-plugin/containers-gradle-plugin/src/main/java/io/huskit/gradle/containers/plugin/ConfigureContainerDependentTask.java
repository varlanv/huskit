package io.huskit.gradle.containers.plugin;

import io.huskit.log.Log;
import io.huskit.gradle.containers.plugin.internal.ContainersTask;
import io.huskit.gradle.containers.plugin.internal.DockerContainersExtension;
import io.huskit.gradle.containers.plugin.internal.buildservice.ContainersBuildService;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

@RequiredArgsConstructor
public class ConfigureContainerDependentTask implements Action<Task> {

    private final Log log;
    private final ProjectDescription projectDescription;
    private final TaskProvider<ContainersTask> containersTaskProvider;
    private final Provider<ContainersBuildService> containersBuildServiceProvider;
    private final DockerContainersExtension dockerContainersExtension;

    public void configure(Task dependentTask) {
        log.info("Adding containers task as dependency to task: [{}]", dependentTask.getName());
        dependentTask.dependsOn(containersTaskProvider);
        dependentTask.mustRunAfter(containersTaskProvider);
        dependentTask.usesService(containersBuildServiceProvider);
        new MaybeAddContainersEnvironment(
                log,
                projectDescription,
                dependentTask,
                containersBuildServiceProvider,
                dockerContainersExtension
        ).maybeAdd();
    }

    @Override
    public void execute(Task task) {
        configure(task);
    }
}
