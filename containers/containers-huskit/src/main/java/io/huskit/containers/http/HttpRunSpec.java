package io.huskit.containers.http;

import io.huskit.common.Mutable;
import io.huskit.containers.api.container.logs.LookFor;
import io.huskit.containers.api.container.run.HtRunSpec;
import io.huskit.containers.api.image.HtImgName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
public class HttpRunSpec implements HtRunSpec {

    @Getter
    HttpCreateSpec createSpec;
    HttpStartSpec startSpec;
    Mutable<Boolean> remove;
    Mutable<LookFor> lookFor;

    public HttpRunSpec(CharSequence dockerImageName) {
        this.createSpec = new HttpCreateSpec(HtImgName.of(dockerImageName));
        this.startSpec = new HttpStartSpec();
        this.remove = Mutable.of(false);
        this.lookFor = Mutable.of(LookFor.nothing());
    }

    @Override
    public HttpRunSpec withLabels(Map<String, ?> labels) {
        createSpec.withLabels(labels);
        return this;
    }

    @Override
    public HttpRunSpec withEnv(Map<String, ?> env) {
        createSpec.withEnv(env);
        return this;
    }

    @Override
    public HttpRunSpec withRemove() {
        createSpec.withRemove();
        return this;
    }

    @Override
    public HttpRunSpec withPortBinding(Number hostPort, Number containerPort) {
        createSpec.withPortBinding(hostPort, containerPort);
        return this;
    }

    @Override
    public HttpRunSpec withPortBindings(Map<? extends Number, ? extends Number> portBindings) {
        createSpec.withPortBindings(portBindings);
        return this;
    }

    @Override
    public HttpRunSpec withCommand(CharSequence command, Object... args) {
        createSpec.withCommand(command, args);
        return this;
    }

    @Override
    public HttpRunSpec withCommand(CharSequence command, Iterable<?> args) {
        createSpec.withCommand(command, args);
        return this;
    }

    @Override
    public HttpRunSpec withLookFor(CharSequence text, Duration timeout) {
        lookFor.set(LookFor.word(text.toString()).withTimeout(timeout));
        return this;
    }

    public LookFor lookFor() {
        return this.lookFor.require();
    }
}
