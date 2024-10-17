package io.huskit.containers.http;

import io.huskit.containers.api.container.logs.HtFollowedLogs;
import io.huskit.containers.api.container.logs.HtLogs;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HttpLogs implements HtLogs {

    HtHttpDockerSpec dockerSpec;
    String containerId;

    public HttpLogs(HtHttpDockerSpec dockerSpec, CharSequence containerId) {
        this.dockerSpec = dockerSpec;
        this.containerId = containerId.toString();
    }

    @Override
    public Stream<String> stream() {
        return streamAsync().join();
    }

    @Override
    public CompletableFuture<Stream<String>> streamAsync() {
        return dockerSpec.socket().sendAsync(
                        new DockerSocket.Request<>(
                                dockerSpec.requests().get(new HttpLogsSpec(containerId)),
                                r -> Arrays.stream(r.string().split("\n"))
                                        .map(String::trim)
                                        .collect(Collectors.toList())
                        ).withExpectedStatus(200)
                )
                .thenApply(response -> response.body().stream());
    }

    @Override
    public HtFollowedLogs follow() {
        return new HttpFollowedLogs(
                dockerSpec,
                new HttpLogsSpec(containerId).withFollow(true)
        );
    }
}
