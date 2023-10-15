package io.huskit.gradle.containers.plugin;

import io.huskit.gradle.common.plugin.model.NewOrExistingExtension;
import io.huskit.gradle.containers.plugin.api.ContainersExtension;
import io.huskit.gradle.containers.plugin.internal.DockerContainersExtension;
import io.huskit.log.Log;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PrepareContainersExtension {

    private final Log log;
    private final ProjectDescription projectDescription;
    private final NewOrExistingExtension newOrExistingExtension;

    public DockerContainersExtension prepare() {
        log.info("Adding containers extension: [{}]", ContainersExtension.name());
        DockerContainersExtension extension = newOrExistingExtension.getOrCreate(
                ContainersExtension.class,
                DockerContainersExtension.class,
                ContainersExtension.name()
        );
        extension.getProjectName().set(projectDescription.name());
        extension.getProjectPath().set(projectDescription.path());
        return extension;
    }
}
