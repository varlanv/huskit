package io.huskit.containers.api.container;

import io.huskit.common.reactive.One;
import io.huskit.containers.api.container.exec.HtExec;
import io.huskit.containers.api.container.list.HtListContainers;
import io.huskit.containers.api.container.list.arg.HtListContainersArgsSpec;
import io.huskit.containers.api.container.logs.HtLogs;
import io.huskit.containers.api.container.rm.HtRm;
import io.huskit.containers.api.container.run.HtCreateSpec;
import io.huskit.containers.api.container.run.HtRmSpec;
import io.huskit.containers.api.container.run.HtRun;
import io.huskit.containers.api.container.run.HtRunSpec;

import java.util.function.Consumer;
import java.util.stream.Stream;

public interface HtContainers {

    HtListContainers list();

    HtListContainers list(Consumer<HtListContainersArgsSpec> argsAction);

    One<Stream<HtContainer>> inspect(Iterable<? extends CharSequence> containerIds);

    One<HtContainer> inspect(CharSequence containerId);

    HtLogs logs(CharSequence containerId);

    HtRun run(CharSequence dockerImageName);

    HtRun run(CharSequence dockerImageName, Consumer<HtRunSpec> specAction);

    HtCreate create(CharSequence dockerImageName);

    HtCreate create(CharSequence dockerImageName, Consumer<HtCreateSpec> specAction);

    HtStart start(CharSequence containerId);

    HtExec execInContainer(CharSequence containerId, CharSequence command, Iterable<? extends CharSequence> args);

    HtExec execInContainer(CharSequence containerId, CharSequence command);

    HtRm remove(CharSequence... containerIds);

    HtRm remove(CharSequence containerId, Consumer<HtRmSpec> specAction);

    HtRm remove(Iterable<? extends CharSequence> containerIds, Consumer<HtRmSpec> specAction);
}
