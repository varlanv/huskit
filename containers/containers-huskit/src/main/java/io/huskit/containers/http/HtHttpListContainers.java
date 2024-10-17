package io.huskit.containers.http;

import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtJsonContainer;
import io.huskit.containers.api.container.list.HtListContainers;
import io.huskit.containers.internal.HtJson;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
class HtHttpListContainers implements HtListContainers {

    HtHttpDockerSpec dockerSpec;
    HtHttpListContainersSpec spec;

    @Override
    public Stream<HtContainer> asStream() {
        return asStreamAsync().join();
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
        return dockerSpec.socket().sendAsync(
                        new DockerSocket.Request<>(
                                dockerSpec.requests().get(spec),
                                response -> HtJson.toMapList(
                                        response.reader()
                                )
                        )
                )
                .thenApply(response ->
                        action.apply(
                                response.body()
                                        .stream()
                                        .map(HtJsonContainer::new)
                        )
                );
    }
}
