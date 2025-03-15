package io.huskit.containers.api.container.rm;

import io.huskit.common.reactive.One;

public interface HtRm {

    One<Void> exec();
}
