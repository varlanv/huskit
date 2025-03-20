package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingSupplier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface OneFrom {

    <T> One<T> empty();

    <T> One<T> item(T item);

    <T> One<T> optional(ThrowingSupplier<Optional<T>> supplier);

    <T> One<T> completion(CompletableFuture<T> completableFuture);

    <T> One<T> emitter(ThrowingConsumer<OneEmitter<T>> emitterConsumer);

    <T> One<T> error(Throwable throwable);
}