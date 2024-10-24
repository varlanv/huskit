package io.huskit.containers.api.container.run;

import io.huskit.containers.api.container.HtContainer;

import java.util.concurrent.CompletableFuture;

public interface HtRun {

    HtContainer exec();

    CompletableFuture<HtContainer> execAsync();
}
