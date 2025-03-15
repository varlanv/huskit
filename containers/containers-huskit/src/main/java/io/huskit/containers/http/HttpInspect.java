package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtJsonContainer;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
final class HttpInspect {

    HtHttpDockerSpec dockerSpec;

    public One<Stream<HtContainer>> inspect(Iterable<? extends CharSequence> containerIds) {
        return One.from().item("").map(ignore -> StreamSupport.stream(containerIds.spliterator(), false)
            .map(this::inspect)
            .map(One::block));
    }

    public One<HtContainer> inspect(CharSequence id) {
        return dockerSpec.socket().send(
            PushIn.of(
                new Request(
                    dockerSpec.requests().get(new HttpInspectSpec(id))
                ).withExpectedStatus(200),
                new PushJsonObject()
            )
        ).map(
            response ->
                new HtJsonContainer(
                    response.body().value()
                )
        );
    }
}
