package io.huskit.containers.api.image;

import io.huskit.common.reactive.One;

public interface HtPullImages {

    One<Void> exec();
}
