package io.huskit.containers.http;

import io.huskit.common.concurrent.FinishFuture;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.container.logs.HtFollowedLogs;
import io.huskit.containers.api.container.logs.LookFor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class HttpFollowedLogs implements HtFollowedLogs {

    HtHttpDockerSpec dockerSpec;
    HttpLogsSpec logsSpec;

    @Override
    public MultiplexedFrames stream() {
        return FinishFuture.finish(streamAsyncInternal(), dockerSpec.defaultTimeout());
    }

    @Override
    public CompletableFuture<MultiplexedFrames> streamAsync() {
        return streamAsyncInternal();
    }

    @Override
    public Stream<String> streamStdOut() {
        return FinishFuture.finish(this.streamStdOutAsync(), dockerSpec.defaultTimeout());
    }

    @Override
    public CompletableFuture<Stream<String>> streamStdOutAsync() {
        return this.streamAsync()
            .thenApply(
                logs -> logs.list().stream()
                    .filter(frame -> frame.type() == FrameType.STDOUT)
                    .map(MultiplexedFrame::stringData)
            );
    }

    @Override
    public Stream<String> streamStdErr() {
        return FinishFuture.finish(streamStdErrAsync(), dockerSpec.defaultTimeout());
    }

    @Override
    public CompletableFuture<Stream<String>> streamStdErrAsync() {
        return this.streamAsync()
            .thenApply(
                logs -> logs.list().stream()
                    .filter(frame -> frame.type() == FrameType.STDERR)
                    .map(MultiplexedFrame::stringData)
            );
    }

    @Override
    public MultiplexedFrames lookFor(LookFor lookFor) {
        return FinishFuture.finish(lookForAsync(lookFor), dockerSpec.defaultTimeout());
    }

    @Override
    @SneakyThrows
    public CompletableFuture<MultiplexedFrames> lookForAsync(LookFor lookFor) {
        var timeout = lookFor.timeout();
        if (timeout.isZero()) {
            return streamAsyncInternal(() -> new PushMultiplexedStream(StreamType.ALL, frame -> lookFor.predicate().test(frame.stringData())));
        } else {
            return streamAsyncInternal(() -> new PushMultiplexedStream(StreamType.ALL, frame -> lookFor.predicate().test(frame.stringData())));
        }
    }

    private CompletableFuture<MultiplexedFrames> streamAsyncInternal() {
        return this.streamAsyncInternal();
    }

    private CompletableFuture<MultiplexedFrames> streamAsyncInternal(Supplier<PushMultiplexedStream> requestAction) {
        return dockerSpec.socket()
            .sendPushAsync(
                PushIn.of(
                    new Request(
                        dockerSpec.requests().get(logsSpec)
                    ).withExpectedStatus(200).withDirtiesConnection(true),
                    requestAction.get()
                )
            )
            .thenApply(
                response -> response.body().value()
            );
    }
}
