package io.huskit.containers.http;

import io.huskit.common.HtConstants;
import io.huskit.common.reactive.One;
import io.huskit.containers.api.container.HtContainer;
import io.huskit.containers.api.container.HtContainers;
import io.huskit.containers.api.container.exec.HtExec;
import io.huskit.containers.api.container.list.HtListContainers;
import io.huskit.containers.api.container.list.arg.HtListContainersArgsSpec;
import io.huskit.containers.api.container.rm.HtRm;
import io.huskit.containers.api.container.run.HtCreateSpec;
import io.huskit.containers.api.container.run.HtRmSpec;
import io.huskit.containers.api.container.run.HtRun;
import io.huskit.containers.api.container.run.HtRunSpec;
import io.huskit.containers.api.image.HtImages;
import io.huskit.containers.api.image.HtImgName;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RequiredArgsConstructor
final class HtHttpContainers implements HtContainers {

    HtHttpDockerSpec dockerSpec;
    HtImages images;

    @Override
    public HtListContainers list() {
        return list(HtConstants.Consumers.noop());
    }

    @Override
    public HtHttpListContainers list(Consumer<HtListContainersArgsSpec> argsAction) {
        var spec = new HtHttpListContainersSpec();
        argsAction.accept(spec);
        return new HtHttpListContainers(dockerSpec, spec);
    }

    @Override
    public One<Stream<HtContainer>> inspect(Iterable<? extends CharSequence> containerIds) {
        return new HttpInspect(dockerSpec).inspect(containerIds);
    }

    @Override
    public One<HtContainer> inspect(CharSequence containerId) {
        return new HttpInspect(dockerSpec).inspect(containerId);
    }

    @Override
    public HttpLogs logs(CharSequence containerId) {
        return new HttpLogs(dockerSpec, containerId);
    }

    @Override
    public HtRun run(CharSequence dockerImageName) {
        return run(
            dockerImageName,
            HtConstants.Consumers.noop()
        );
    }

    @Override
    public HtRun run(CharSequence dockerImageName, Consumer<HtRunSpec> specAction) {
        var spec = new HttpRunSpec(dockerImageName);
        specAction.accept(spec);
        return new HttpRun(
            new HttpCreate(
                dockerImageName.toString(),
                dockerSpec,
                spec.createSpec(),
                new HttpInspect(dockerSpec),
                new LocalImagesStash(images, dockerSpec.log())
            ),
            spec,
            this::start,
            this::logs,
            dockerSpec
        );
    }

    @Override
    public HttpCreate create(CharSequence dockerImageName) {
        return create(
            dockerImageName,
            HtConstants.Consumers.noop()
        );
    }

    @Override
    public HttpCreate create(CharSequence dockerImageName, Consumer<HtCreateSpec> specAction) {
        var spec = new HttpCreateSpec(HtImgName.of(dockerImageName));
        specAction.accept(spec);
        return new HttpCreate(
            dockerImageName.toString(),
            dockerSpec,
            spec,
            new HttpInspect(dockerSpec),
            new LocalImagesStash(images, dockerSpec.log())
        );
    }

    @Override
    public HttpStart start(CharSequence containerId) {
        return new HttpStart(
            dockerSpec,
            new HttpStartSpec(),
            containerId.toString()
        );
    }

    @Override
    public HtExec execInContainer(CharSequence containerId, CharSequence command, Iterable<? extends CharSequence> args) {
        return new HttpExec(
            dockerSpec,
            new HttpExecSpec(
                containerId,
                command,
                args
            )
        );
    }

    @Override
    public HtExec execInContainer(CharSequence containerId, CharSequence command) {
        return execInContainer(containerId, command, List.of());
    }

    @Override
    public HtRm remove(CharSequence... containerIds) {
        return remove(
            Arrays.asList(containerIds),
            HtConstants.Consumers.noop()
        );
    }

    @Override
    public HtRm remove(CharSequence containerId, Consumer<HtRmSpec> specAction) {
        return remove(Collections.singletonList(containerId), specAction);
    }

    @Override
    public HtRm remove(Iterable<? extends CharSequence> containerIds, Consumer<HtRmSpec> specAction) {
        var spec = new HttpRmSpec();
        specAction.accept(spec);
        return new HttpRm(dockerSpec, spec, containerIds);
    }
}
