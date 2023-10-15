package io.huskit.gradle.containers.plugin;

import io.huskit.gradle.common.plugin.model.NewOrExistingExtension;
import io.huskit.log.Log;
import lombok.RequiredArgsConstructor;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.tasks.TaskContainer;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class ConfigureContainersPlugin {

    private final Log log;
    private final ProjectDescription projectDescription;
    private final ObjectFactory objects;
    private final NewOrExistingExtension extensions;
    private final BuildServiceRegistry sharedServices;
    private final TaskContainer tasks;
    private final Consumer<Action<Project>> afterEvaluateSupplier;

    public void configure() {
        var dockerContainersExtension = new PrepareContainersExtension(
                log,
                projectDescription,
                extensions
        ).prepare();
        var containersBuildServiceProvider = new RegisterContainersBuildService(
                log,
                sharedServices
        ).register();
        afterEvaluateSupplier.accept(
                new ConfigureContainersAfterProjectEvaluate(
                        log,
                        projectDescription,
                        dockerContainersExtension,
                        tasks,
                        containersBuildServiceProvider
                )
        );
    }
}
