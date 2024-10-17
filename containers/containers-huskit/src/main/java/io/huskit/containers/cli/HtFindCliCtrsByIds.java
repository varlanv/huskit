package io.huskit.containers.cli;

import io.huskit.common.HtStrings;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.internal.HtJson;
import io.huskit.containers.api.container.HtJsonContainer;
import io.huskit.containers.model.CommandType;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
class HtFindCliCtrsByIds {

    HtCli cli;
    Set<String> ids;

    @SneakyThrows
    public Stream<HtContainer> stream() {
        try {
            return cli.sendCommand(
                    new CliCommand(
                            CommandType.CONTAINERS_INSPECT,
                            buildListContainersCommand(ids)
                    ).withLinePredicate(Predicate.not(String::isBlank)),
                    result -> {
                        return result.lines().stream()
                                .map(HtJson::toMap)
                                .map(HtJsonContainer::new);
                    }
            );
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Failed to find containers by ids - %s", ids), e);
        }
    }

    private List<String> buildListContainersCommand(Set<String> ids) {
        var staticArgsSize = 4;
        var command = new ArrayList<String>(staticArgsSize + ids.size());
        command.add("docker");
        command.add("inspect");
        command.add("--format");
        command.add(HtStrings.doubleQuote("{{json .}}"));
        command.addAll(ids);
        return command;
    }
}
