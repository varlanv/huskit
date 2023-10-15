package io.huskit.gradle.containers.plugin;

import io.huskit.log.Log;
import io.huskit.gradle.containers.plugin.internal.ContainersTask;
import io.huskit.gradle.containers.plugin.internal.DockerContainersExtension;
import io.huskit.gradle.containers.plugin.internal.ShouldStartBeforeSpec;
import io.huskit.gradle.containers.plugin.internal.buildservice.ContainersBuildService;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.Optional;

@RequiredArgsConstructor
public class ConfigureContainersAfterProjectEvaluate implements Action<Project> {

    private final Log log;
    private final ProjectDescription projectDescription;
    private final DockerContainersExtension dockerContainersExtension;
    private final TaskContainer tasks;
    private final Provider<ContainersBuildService> containersBuildServiceProvider;

    @Override
    public void execute(Project ignored) {
        Optional.ofNullable(dockerContainersExtension.getShouldStartBeforeSpec().getOrNull())
                .ifPresentOrElse(shouldStartBeforeSpec -> {
                    if (shouldStartBeforeSpec.isSet()) {
                        getShouldRunBeforeTaskProvider(shouldStartBeforeSpec)
                                .or(() -> getTaskTaskProviderFromTaskName(shouldStartBeforeSpec))
                                .ifPresentOrElse(dependentTaskProvider -> {
                                    var containersTaskProvider = new RegisterContainersTask(
                                            log,
                                            projectDescription,
                                            tasks,
                                            dockerContainersExtension,
                                            containersBuildServiceProvider,
                                            dependentTaskProvider.getName()
                                    ).register();
                                    dependentTaskProvider.configure(configureContainerDependentTaskAction(containersTaskProvider));
                                }, () -> {
                                    var dependentTask = shouldStartBeforeSpec.getShouldRunBeforeTask().get();
                                    var containersTask = new RegisterContainersTask(
                                            log,
                                            projectDescription,
                                            tasks,
                                            dockerContainersExtension,
                                            containersBuildServiceProvider,
                                            dependentTask.getName()
                                    ).register();
                                    var configureContainerDependentTask = configureContainerDependentTaskAction(containersTask);
                                    configureContainerDependentTask.configure(dependentTask);
                                });
                    } else {
                        log.info("No containers will be started, because shouldStartBefore is set to false");
                    }
                }, () -> log.info("No containers will be started, because no task was specified to run before containers start"));
    }

    private Optional<TaskProvider<Task>> getTaskTaskProviderFromTaskName(ShouldStartBeforeSpec shouldStartBeforeSpec) {
        String taskName = shouldStartBeforeSpec.getShouldRunBeforeTaskName().getOrNull();
        if (taskName != null) {
            log.info("Using task name to find task provider for shouldRunBeforeTask: [{}]", taskName);
            return Optional.of(tasks.named(taskName));
        } else {
            log.info("No task name found for shouldRunBeforeTask");
            return Optional.empty();
        }
    }

    private Optional<TaskProvider<Task>> getShouldRunBeforeTaskProvider(ShouldStartBeforeSpec shouldStartBeforeSpec) {
        TaskProvider<Task> provider = shouldStartBeforeSpec.getShouldRunBeforeTaskProvider().getOrNull();
        if (provider != null) {
            log.info("Found task provider for shouldRunBeforeTask: [{}]", provider.getName());
            return Optional.of(provider);
        } else {
            log.info("No task provider found for shouldRunBeforeTask");
            return Optional.empty();
        }
    }

    private ConfigureContainerDependentTask configureContainerDependentTaskAction(TaskProvider<ContainersTask> containersTask) {
        return new ConfigureContainerDependentTask(
                log,
                projectDescription,
                containersTask,
                containersBuildServiceProvider,
                dockerContainersExtension
        );
    }
}
