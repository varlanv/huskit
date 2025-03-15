package io.huskit.containers.http;

import io.huskit.common.reactive.One;
import io.huskit.common.reactive.PushIn;
import io.huskit.containers.api.container.logs.HtFollowedLogs;
import io.huskit.containers.api.container.logs.HtLogs;

import java.util.stream.Stream;

final class HttpLogs implements HtLogs {

    HtHttpDockerSpec dockerSpec;
    String containerId;

    public HttpLogs(HtHttpDockerSpec dockerSpec, CharSequence containerId) {
        this.dockerSpec = dockerSpec;
        this.containerId = containerId.toString();
    }

    @Override
    public One<MultiplexedFrames> frames() {
        return asyncStreamOpen();
    }

    @Override
    public One<Stream<String>> stdOut() {
        return asyncStreamOpen()
            .map(MultiplexedFrames::stdOut);
    }

    @Override
    public One<Stream<String>> stdErr() {
        return asyncStreamOpen()
            .map(MultiplexedFrames::stdErr);
    }

    @Override
    public HtFollowedLogs follow() {
        return new HttpFollowedLogs(
            dockerSpec,
            new HttpLogsSpec(containerId).withFollow(true)
        );
    }

    private One<MultiplexedFrames> asyncStreamOpen() {
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
            )
            .map(response -> response.body().value());
    }
}
