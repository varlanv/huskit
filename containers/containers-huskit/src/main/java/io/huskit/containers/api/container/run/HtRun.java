package io.huskit.containers.api.container.run;

import io.huskit.common.reactive.One;
import io.huskit.containers.api.container.HtContainer;

public interface HtRun {

    One<HtContainer> exec();
}
