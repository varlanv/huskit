package io.huskit.containers.api.container.logs;

import io.huskit.common.reactive.One;
import io.huskit.containers.http.MultiplexedFrames;

import java.util.stream.Stream;

public interface HtFollowedLogs {

    One<MultiplexedFrames> stream();

    One<Stream<String>> streamStdOut();

    One<Stream<String>> streamStdErr();

    One<MultiplexedFrames> lookFor(LookFor lookFor);
}
