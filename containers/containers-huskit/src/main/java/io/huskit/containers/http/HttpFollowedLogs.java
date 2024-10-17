package io.huskit.containers.http;

import io.huskit.common.Mutable;
import io.huskit.containers.api.container.logs.HtFollowedLogs;
import io.huskit.containers.api.container.logs.LookFor;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
class HttpFollowedLogs implements HtFollowedLogs {

    HtHttpDockerSpec dockerSpec;
    HttpLogsSpec logsSpec;
    Mutable<LookFor> lookFor = Mutable.of();

    @Override
    public Stream<String> stream() {
        var words = new ArrayList<String>();
        return lookFor.mapOr(
                lf -> {
                    dockerSpec.socket().send(
                            new DockerSocket.Request<>(
                                    dockerSpec.requests().get(logsSpec),
                                    r -> List.of()
                            ).withRepeatReadPredicate(
                                    line -> {
                                        var word = line.trim();
                                        words.add(word);
                                        return lf.predicate().test(word);
                                    },
                                    Duration.ofMillis(10)
                            ).withExpectedStatus(200)
                    );
                    return words.stream();
                },
                () -> dockerSpec.socket()
                        .send(
                                new DockerSocket.Request<>(
                                        dockerSpec.requests().get(logsSpec),
                                        r -> Arrays.stream(r.string().split("\n"))
                                                .map(String::trim)
                                                .collect(Collectors.toList())
                                ).withExpectedStatus(200)
                        )
                        .body()
                        .stream());
    }

    @Override
    public HtFollowedLogs lookFor(LookFor lookFor) {
        this.lookFor.set(lookFor);
        return this;
    }
}
