package io.huskit.gradle.containers.plugin.api;

import io.huskit.containers.model.id.ContainerId;
import io.huskit.gradle.containers.plugin.GradleProjectDescription;
import io.huskit.gradle.containers.plugin.ProjectDescription;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

public interface ContainerRequestedByUserForTask extends ContainerRequestedByUser {

    ContainerId id();

    @Internal
    Property<String> getProjectPath();

    @Internal
    Property<String> getProjectName();

    default ProjectDescription projectDescription() {
        return new GradleProjectDescription(
                getProjectPath().get(),
                getProjectName().get()
        );
    }
}
