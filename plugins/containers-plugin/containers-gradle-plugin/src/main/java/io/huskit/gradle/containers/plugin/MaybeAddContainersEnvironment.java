package io.huskit.gradle.containers.plugin;

import io.huskit.log.Log;
import io.huskit.gradle.containers.plugin.internal.AddContainersEnvironment;
import io.huskit.gradle.containers.plugin.internal.DockerContainersExtension;
import io.huskit.gradle.containers.plugin.internal.buildservice.ContainersBuildService;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;

@RequiredArgsConstructor
public class MaybeAddContainersEnvironment {

    private final Log log;
    private final ProjectDescription projectDescription;
    private final Task dependentTask;
    private final Provider<ContainersBuildService> containersBuildServiceProvider;
    private final DockerContainersExtension dockerContainersExtension;

    public void maybeAdd() {
        log.info("Adding containers environment to task: [{}]", dependentTask.getName());
        dependentTask.doFirst(
                new AddContainersEnvironment(
                        log,
                        projectDescription,
                        containersBuildServiceProvider,
                        dockerContainersExtension.getContainersRequestedByUser()
                )
        );
    }
}
