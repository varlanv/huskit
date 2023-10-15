package io.huskit.gradle.containers.plugin;

import io.huskit.gradle.common.plugin.model.NewOrExistingExtension;
import io.huskit.log.GradleProjectLog;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class HuskitContainersPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        var extensions = project.getExtensions();
        var objects = project.getObjects();
        var tasks = project.getTasks();
        var projectPath = project.getPath();
        var projectName = project.getName();
        var projectDescription = new GradleProjectDescription(
                projectPath,
                projectName
        );
        var log = new GradleProjectLog(
                HuskitContainersPlugin.class,
                projectPath,
                projectName
        );
        new ConfigureContainersPlugin(
                log,
                projectDescription,
                objects,
                new NewOrExistingExtension(
                        log,
                        extensions
                ),
                project.getGradle().getSharedServices(),
                tasks,
                project::afterEvaluate
        ).configure();
    }
}
