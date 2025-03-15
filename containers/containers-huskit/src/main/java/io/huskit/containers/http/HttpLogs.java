package io.huskit.containers.http;

import io.huskit.common.concurrent.FinishFuture;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.container.logs.HtFollowedLogs;
import io.huskit.containers.api.container.logs.HtLogs;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

final class HttpLogs implements HtLogs {

    HtHttpDockerSpec dockerSpec;
    String containerId;

    public HttpLogs(HtHttpDockerSpec dockerSpec, CharSequence containerId) {
        this.dockerSpec = dockerSpec;
        this.containerId = containerId.toString();
    }

    @Override
    public MultiplexedFrames frames() {
        return FinishFuture.finish(asyncFrames(), dockerSpec.defaultTimeout());
    }

    @Override
    public CompletableFuture<MultiplexedFrames> asyncFrames() {
        return asyncStreamOpen();
    }

    @Override
    public Stream<String> stdOut() {
        return FinishFuture.finish(asyncStdOut(), dockerSpec.defaultTimeout());
    }

    @Override
    public CompletableFuture<Stream<String>> asyncStdOut() {
        return asyncStreamOpen()
            .thenApply(
                MultiplexedFrames::stdOut
            );
    }

    @Override
    public Stream<String> stdErr() {
        return FinishFuture.finish(asyncStdErr(), dockerSpec.defaultTimeout());
    }

    @Override
    public CompletableFuture<Stream<String>> asyncStdErr() {
        return asyncStreamOpen()
            .thenApply(
                MultiplexedFrames::stdErr
            );
    }

    @Override
    public HtFollowedLogs follow() {
        return new HttpFollowedLogs(
            dockerSpec,
            new HttpLogsSpec(containerId).withFollow(true)
        );
    }

    private CompletableFuture<MultiplexedFrames> asyncStreamOpen() {
        return dockerSpec.socket()
            .sendPushAsync(
                PushIn.of(
                    new Request(
                        dockerSpec.requests().get(
                            new HttpLogsSpec(containerId)
                        )
                    ).withExpectedStatus(200),
                    new PushMultiplexedStream(
                        StreamType.ALL
                    )
                )
            ).thenApply(
                response -> response.body().value()
            );
    }
}
