package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingSupplier;
import lombok.SneakyThrows;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

final class DfOneFrom implements OneFrom {

    static final OneFrom INSTANCE = new DfOneFrom();

    @Override
    @SuppressWarnings("unchecked")
    public <T> One<T> empty() {
        return (One<T>) OneEmpty.INSTANCE;
    }

    @Override
    public <T> One<T> item(T item) {
        return new OneFromItem<>(item);
    }

    @Override
    @SneakyThrows
    public <T> One<T> optional(ThrowingSupplier<Optional<T>> supplier) {
        return new OneFromOptional<>(supplier);
    }

    @Override
    public <T> One<T> completion(CompletableFuture<T> completableFuture) {
        return new OneFromCompletion<>(completableFuture);
    }

    @Override
    public <T> One<T> emitter(ThrowingConsumer<OneEmitter<T>> emitterConsumer) {
        return new OneFromEmitter<T>(emitterConsumer);
    }

    @Override
    public <T> One<T> error(Throwable throwable) {
        return new OneFromError<>(throwable);
    }
}
