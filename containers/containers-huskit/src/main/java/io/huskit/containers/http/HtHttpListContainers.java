package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtJsonContainer;
import io.huskit.containers.api.container.list.HtListContainers;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class HtHttpListContainers implements HtListContainers {

    HtHttpDockerSpec dockerSpec;
    HtHttpListContainersSpec spec;

    public One<List<HtContainer>> asList() {
        return send(s -> s.collect(Collectors.toList()));
    }

    public One<Stream<HtContainer>> asStream() {
        return send(s -> s.map(Function.identity()));
    }

    private <R> One<R> send(Function<Stream<HtJsonContainer>, R> action) {
        return dockerSpec.socket()
            .send(
                PushIn.of(
                    new Request(dockerSpec.requests().get(spec)),
                    new PushJsonArray()
                )
            )
            .map(
                response ->
                    action.apply(
                        response.body().value().stream()
                            .map(HtJsonContainer::new)
                    )
            );
    }
}
