package io.huskit.containers.api.container.list;

import io.huskit.containers.api.container.HtContainer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface HtListContainers {

    Stream<HtContainer> asStream();

    List<HtContainer> asList();

    CompletableFuture<List<HtContainer>> asListAsync();

    CompletableFuture<Stream<HtContainer>> asStreamAsync();
}
