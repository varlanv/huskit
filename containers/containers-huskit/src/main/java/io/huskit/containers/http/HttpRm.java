package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.common.reactive.PushOut;
import io.huskit.containers.api.container.rm.HtRm;
import lombok.RequiredArgsConstructor;

import java.util.Spliterators;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
final class HttpRm implements HtRm {

    HtHttpDockerSpec dockerSpec;
    HttpRmSpec spec;
    Iterable<? extends CharSequence> containerIds;

    @Override
    public One<Void> exec() {
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(containerIds.iterator(), 0), false)
            .map(containerId -> dockerSpec.socket().send(
                PushIn.of(
                    new Request(
                        spec.toRequest(containerId)
                    ).withExpectedStatus(204),
                    PushOut.ready(true)
                )
            ))
            .forEach(One::block);
        return One.from().empty().mapToNothing();
    }
}
