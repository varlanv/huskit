package io.huskit.containers.api.container;

import java.util.concurrent.CompletableFuture;

public interface HtCreate {

    HtContainer exec();

    CompletableFuture<HtContainer> execAsync();
}
