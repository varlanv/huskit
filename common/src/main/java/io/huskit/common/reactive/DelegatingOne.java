package io.huskit.common.reactive;

import java.util.concurrent.CompletableFuture;

interface DelegatingOne<T> extends One<T> {

    One<T> delegate();

    @Override
    default CompletableFuture<T> toFuture() {
        var future = new CompletableFuture<T>();
        try {
            delegate().subscribe(future::complete);
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
        return future;
    }
}
