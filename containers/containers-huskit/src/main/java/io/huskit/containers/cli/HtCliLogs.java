package io.huskit.containers.cli;

import io.huskit.containers.api.container.logs.HtFollowedLogs;
import io.huskit.containers.api.container.logs.HtLogs;
import io.huskit.containers.api.container.logs.LookFor;
import io.huskit.containers.model.CommandType;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@With
@RequiredArgsConstructor
public class HtCliLogs implements HtLogs {

    HtCli cli;
    String id;

    @Override
    public Stream<String> stream() {
        return Stream.of("")
                .flatMap(ignore -> cli.sendCommand(
                        new CliCommand(
                                CommandType.CONTAINERS_LOGS,
                                List.of("docker", "logs", id)
                        ),
                        CommandResult::lines
                ).stream());
    }

    @Override
    public CompletableFuture<Stream<String>> streamAsync() {
        return CompletableFuture.completedFuture(stream());
    }

    @Override
    public HtFollowedLogs follow() {
        return new HtCliFollowedLogs(cli, id, LookFor.nothing());
    }
}
