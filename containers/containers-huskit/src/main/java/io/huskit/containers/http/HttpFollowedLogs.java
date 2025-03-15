package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.container.logs.HtFollowedLogs;
import io.huskit.containers.api.container.logs.LookFor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.function.Supplier;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class HttpFollowedLogs implements HtFollowedLogs {

    HtHttpDockerSpec dockerSpec;
    HttpLogsSpec logsSpec;

    @Override
    public One<MultiplexedFrames> stream() {
        return streamAsyncInternal(() -> new PushMultiplexedStream(StreamType.ALL, frame -> true));
    }

    @Override
    public One<Stream<String>> streamStdOut() {
        return this.stream()
            .map(
                logs -> logs.list().stream()
                    .filter(frame -> frame.type() == FrameType.STDOUT)
                    .map(MultiplexedFrame::stringData)
            );
    }

    @Override
    public One<Stream<String>> streamStdErr() {
        return this.stream()
            .map(
                logs -> logs.list().stream()
                    .filter(frame -> frame.type() == FrameType.STDERR)
                    .map(MultiplexedFrame::stringData)
            );
    }

    @Override
    @SneakyThrows
    public One<MultiplexedFrames> lookFor(LookFor lookFor) {
        var timeout = lookFor.timeout();
        if (timeout.isZero()) {
            return streamAsyncInternal(() -> new PushMultiplexedStream(StreamType.ALL, frame -> lookFor.predicate().test(frame.stringData())));
        } else {
            return streamAsyncInternal(() -> new PushMultiplexedStream(StreamType.ALL, frame -> lookFor.predicate().test(frame.stringData())));
        }
    }

    private One<MultiplexedFrames> streamAsyncInternal(Supplier<PushMultiplexedStream> requestAction) {
        return dockerSpec.socket()
            .sendPushAsync(
                PushIn.of(
                    new Request(
                        dockerSpec.requests().get(logsSpec)
                    ).withExpectedStatus(200).withDirtiesConnection(true),
                    requestAction.get()
                )
            )
            .map(response -> response.body().value());
    }
}
