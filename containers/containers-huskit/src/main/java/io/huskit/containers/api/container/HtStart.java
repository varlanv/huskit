package io.huskit.containers.api.container;

import io.huskit.common.reactive.One;

public interface HtStart {

    One<HtContainer> execAsync();
}
