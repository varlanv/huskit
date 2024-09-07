package io.huskit.gradle.containers.plugin.internal;

import io.huskit.containers.model.ProjectDescription;
import io.huskit.containers.model.started.StartedContainer;
import io.huskit.gradle.common.plugin.model.string.CapitalizedString;
import io.huskit.gradle.containers.plugin.api.ContainerRequestSpec;
import io.huskit.gradle.containers.plugin.internal.buildservice.ContainersBuildService;
import io.huskit.log.GradleProjectLog;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.util.List;
import java.util.stream.Collectors;

@DisableCachingByDefault(because = "Caching of containers is not supported")
public abstract class ContainersTask extends DefaultTask {

    private static final String NAME = "startContainers";

    public static String nameForTask(String taskName) {
        return ContainersTask.NAME + "For" + CapitalizedString.capitalize(taskName);
    }

    public static String doFirstActionName() {
        return "startContainers";
    }

    @Internal
    public abstract Property<ContainersBuildService> getContainersBuildService();

    @Internal
    public abstract Property<ProjectDescription> getProjectDescription();

    @Input
    public abstract ListProperty<ContainerRequestSpec> getRequestedContainers();


    @TaskAction
    public void startContainers() {
        startAndReturnContainers();
    }

    public List<StartedContainer> startAndReturnContainers() {
        var containerRequestSpecs = getRequestedContainers().get();
        if (containerRequestSpecs.isEmpty()) {
            return List.of();
        }
        var projectDescription = getProjectDescription().get();
        var log = new GradleProjectLog(
                ContainersTask.class,
                projectDescription.path(),
                projectDescription.name()
        );
        var startedContainers = getContainersBuildService().get().containers(
                new ContainersRequestV2(
                        log,
                        projectDescription,
                        getRequestedContainers()
                )
        ).list();
        if (startedContainers.isEmpty()) {
            log.info("No containers were started");
        } else {
            log.info("Started [{}] containers: [{}]", startedContainers.size(), startedContainers.stream()
                    .map(StartedContainer::id)
                    .collect(Collectors.toList())
            );
        }
        return startedContainers;
    }
}
