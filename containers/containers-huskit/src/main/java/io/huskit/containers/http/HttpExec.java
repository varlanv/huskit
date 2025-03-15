package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.common.reactive.PushOut;
import io.huskit.containers.api.container.exec.HtExec;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class HttpExec implements HtExec {

    HtHttpDockerSpec dockerSpec;
    HttpExecSpec httpExecSpec;

    @Override
    public One<Void> exec() {
        return dockerSpec.socket().send(
                PushIn.of(
                    new Request(
                        dockerSpec.requests().post(httpExecSpec)
                    ),
                    PushOut.ready(true)
                )
            )
            .mapToNothing();
    }
}
