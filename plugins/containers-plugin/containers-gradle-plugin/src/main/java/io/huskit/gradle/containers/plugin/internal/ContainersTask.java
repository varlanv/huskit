package io.huskit.gradle.containers.plugin.internal;

import io.huskit.containers.model.started.StartedContainer;
import io.huskit.gradle.containers.plugin.ProjectDescription;
import io.huskit.gradle.containers.plugin.api.ContainerRequestedByUser;
import io.huskit.gradle.containers.plugin.internal.buildservice.ContainersBuildService;
import io.huskit.log.GradleProjectLog;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.util.stream.Collectors;

@DisableCachingByDefault(because = "Caching of containers is not supported")
public abstract class ContainersTask extends DefaultTask {

    public static String NAME = "startContainers";

    @Internal
    public abstract Property<ContainersBuildService> getContainers();

    @Internal
    public abstract Property<ProjectDescription> getProjectDescription();

    @Input
    public abstract ListProperty<ContainerRequestedByUser> getRequestedContainers();

    @TaskAction
    public void startContainers() {
        var projectDescription = getProjectDescription().get();
        var log = new GradleProjectLog(
                ContainersTask.class,
                projectDescription.path(),
                projectDescription.name()
        );
        var requestedContainers = new RequestedContainersFromGradleUser(
                log,
                getRequestedContainers().get()
        );
        var list = getContainers().get().containers(
                projectDescription,
                requestedContainers,
                log
        ).start().list();
        if (list.isEmpty()) {
            log.info("No containers were started");
        } else {
            log.info("Started [{}] containers: [{}]", list.size(), list.stream()
                    .map(StartedContainer::id)
                    .collect(Collectors.toList())
            );
        }
    }
}
