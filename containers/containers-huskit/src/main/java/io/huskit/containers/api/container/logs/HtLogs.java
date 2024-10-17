package io.huskit.containers.api.container.logs;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface HtLogs {

    Stream<String> stream();

    CompletableFuture<Stream<String>> streamAsync();

    HtFollowedLogs follow();
}
