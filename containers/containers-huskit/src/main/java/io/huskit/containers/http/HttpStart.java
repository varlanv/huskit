package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.common.reactive.PushOut;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtLazyContainer;
import io.huskit.containers.api.container.HtStart;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class HttpStart implements HtStart {

    HtHttpDockerSpec dockerSpec;
    HttpStartSpec httpStartSpec;
    String containerId;

    @Override
    public One<HtContainer> execAsync() {
        return dockerSpec.socket().send(
            PushIn.of(new Request(
                    httpStartSpec.toRequest(containerId)
                ).withExpectedStatus(204),
                PushOut.ready(true)
            )
        ).map(
            r -> new HtLazyContainer(
                containerId,
                () -> new HttpInspect(dockerSpec).inspect(containerId).block()
            )
        );
    }
}
