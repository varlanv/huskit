package io.huskit.gradle.containers.plugin.internal.buildservice;

import io.huskit.containers.model.Containers;
import io.huskit.containers.model.request.RequestedContainers;
import io.huskit.gradle.common.plugin.model.DefaultInternalExtensionName;
import io.huskit.gradle.containers.plugin.ProjectDescription;
import io.huskit.gradle.containers.plugin.internal.ContainersApplication;
import io.huskit.gradle.containers.plugin.internal.ContainersBuildServiceParams;
import io.huskit.log.GradleLog;
import io.huskit.log.Log;
import org.gradle.api.services.BuildService;

import java.io.Serializable;

public abstract class ContainersBuildService implements BuildService<ContainersBuildServiceParams>, AutoCloseable, Serializable {

    public static String name() {
        return DefaultInternalExtensionName.value("containers_build_service");
    }

    private transient volatile ContainersApplication containersApplication;

    public Containers containers(ProjectDescription projectDescription,
                                 RequestedContainers requestedContainers,
                                 Log taskLog) {
        return getContainersApplication().containers(
                projectDescription,
                requestedContainers,
                taskLog
        );
    }

    @Override
    public void close() throws Exception {
        getContainersApplication().stop();
    }

    private ContainersApplication getContainersApplication() {
        if (containersApplication == null) {
            synchronized (this) {
                if (containersApplication == null) {
                    var commonLog = new GradleLog(ContainersBuildService.class);
                    commonLog.info("containersApplication is not created, entered synchronized block to create instance");
                    containersApplication = new ContainersApplication(commonLog);
                }
            }
        }
        return containersApplication;
    }
}
