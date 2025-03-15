package io.huskit.containers.api.container.exec;

import io.huskit.common.reactive.One;

public interface HtExec {

    One<Void> exec();
}
