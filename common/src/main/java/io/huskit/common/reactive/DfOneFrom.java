package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;

import java.util.concurrent.CompletableFuture;

final class DfOneFrom implements One.OneFrom {

    static final DfOneFrom INSTANCE = new DfOneFrom();

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
