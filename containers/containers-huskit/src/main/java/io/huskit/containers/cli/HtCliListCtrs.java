package io.huskit.containers.cli;

import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.list.HtListContainers;
import io.huskit.containers.model.CommandType;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@With
@RequiredArgsConstructor
class HtCliListCtrs implements HtListContainers {

    HtCli cli;
    HtCliListCtrsArgsSpec args;

    @Override
    public Stream<HtContainer> asStream() {
        var requestedIds = findIds();
        if (requestedIds.isEmpty()) {
            return Stream.empty();
        } else {
            return new HtFindCliCtrsByIds(
                    cli,
                    requestedIds
            ).stream();
        }
    }

    @Override
    public List<HtContainer> asList() {
        return asStream().collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<HtContainer>> asListAsync() {
        return CompletableFuture.supplyAsync(this::asList);
    }

    @Override
    public CompletableFuture<Stream<HtContainer>> asStreamAsync() {
        return CompletableFuture.supplyAsync(this::asStream);
    }

    private Set<String> findIds() {
        return new LinkedHashSet<>(
                cli.sendCommand(
                        new CliCommand(
                                CommandType.CONTAINERS_LIST,
                                new FindIdsCommand(
                                        args.build()
                                ).list()
                        ),
                        CommandResult::lines
                )
        );
    }
}
