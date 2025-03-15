package io.huskit.containers.integration;

import io.huskit.common.Mutable;
import io.huskit.common.Volatile;
import io.huskit.containers.api.docker.HtDocker;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class DefDockerClientSpec implements DockerClientSpec {

    Mutable<HtServiceContainer> parent = Volatile.of();
    Mutable<HtDocker> docker = Volatile.of();

    public Optional<HtDocker> docker() {
        return docker.maybe();
    }

    @Override
    public HtServiceContainer withDocker(HtDocker docker) {
        this.docker.set(docker);
        return parent.require();
    }

    HtServiceContainer setParent(HtServiceContainer parent) {
        if (this.parent.isPresent()) {
            throw new IllegalStateException("Parent already set");
        }
        this.parent.set(parent);
        return parent;
    }
}
