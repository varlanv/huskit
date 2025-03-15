package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;

import java.util.concurrent.CompletableFuture;

record OneFromCompletion<T>(CompletableFuture<T> completableFuture) implements One<T> {

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        completableFuture.thenAccept(consumer.toUnchecked());
    }
}
