package io.huskit.containers.http;

import io.huskit.common.concurrent.FinishFuture;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtJsonContainer;
import io.huskit.containers.api.container.list.HtListContainers;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class HtHttpListContainers implements HtListContainers {

    HtHttpDockerSpec dockerSpec;
    HtHttpListContainersSpec spec;

    @Override
    @SneakyThrows
    public Stream<HtContainer> asStream() {
        return FinishFuture.finish(asStreamAsync(), dockerSpec.defaultTimeout());
    }

    @Override
    public List<HtContainer> asList() {
        return asStream().collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<HtContainer>> asListAsync() {
        return send(s -> s.collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Stream<HtContainer>> asStreamAsync() {
        return send(s -> s.map(Function.identity()));
    }

    private <R> CompletableFuture<R> send(Function<Stream<HtJsonContainer>, R> action) {
        return dockerSpec.socket().sendPushAsync(
            PushIn.of(
                new Request(
                    dockerSpec.requests().get(spec)
                ),
                new PushJsonArray()
            )
        ).thenApply(
            response ->
                action.apply(
                    response.body().value().stream()
                        .map(HtJsonContainer::new)
                )
        );
    }
}
