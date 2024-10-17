package io.huskit.containers.http;

import io.huskit.containers.api.container.rm.HtRm;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class HttpRm implements HtRm {

    HtHttpDockerSpec dockerSpec;
    HttpRmSpec spec;
    Iterable<? extends CharSequence> containerIds;

    @Override
    public void exec() {
        var ran = false;
        for (var containerId : containerIds) {
            dockerSpec.socket().send(
                    new DockerSocket.Request<>(
                            spec.toRequest(containerId),
                            List::of
                    )
            );
            ran = true;
        }
        if (!ran) {
            throw new IllegalStateException("Received empty container ID list for removal");
        }
    }
}
