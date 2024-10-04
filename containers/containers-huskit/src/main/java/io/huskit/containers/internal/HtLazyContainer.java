package io.huskit.containers.internal;

import io.huskit.containers.api.HtContainer;
import io.huskit.containers.api.HtContainerConfig;
import io.huskit.containers.api.HtContainerNetwork;
import io.huskit.containers.api.HtContainerState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class HtLazyContainer implements HtContainer {

    @Getter
    String id;
    Supplier<HtContainer> delegate;

    @Override
    public String name() {
        return delegate.get().name();
    }

    @Override
    public HtContainerConfig config() {
        return delegate.get().config();
    }

    @Override
    public HtContainerNetwork network() {
        return delegate.get().network();
    }

    @Override
    public Instant createdAt() {
        return delegate.get().createdAt();
    }

    @Override
    public List<String> args() {
        return delegate.get().args();
    }

    @Override
    public String path() {
        return delegate.get().path();
    }

    @Override
    public String processLabel() {
        return delegate.get().processLabel();
    }

    @Override
    public String platform() {
        return delegate.get().platform();
    }

    @Override
    public String driver() {
        return delegate.get().driver();
    }

    @Override
    public String hostsPath() {
        return delegate.get().hostsPath();
    }

    @Override
    public String hostnamePath() {
        return delegate.get().hostnamePath();
    }

    @Override
    public Integer restartCount() {
        return delegate.get().restartCount();
    }

    @Override
    public String mountLabel() {
        return delegate.get().mountLabel();
    }

    @Override
    public HtContainerState state() {
        return delegate.get().state();
    }

    @Override
    public String resolvConfPath() {
        return delegate.get().resolvConfPath();
    }

    @Override
    public String logPath() {
        return delegate.get().logPath();
    }

    @Override
    public String toString() {
        return delegate.get().toString();
    }
}
