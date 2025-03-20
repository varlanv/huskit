package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

final class DfManyFrom implements ManyFrom {

    static final ManyFrom INSTANCE = new DfManyFrom();

    @Override
    public <T> Many<T> item(T item) {
        return null;
    }

    @Override
    public <T> Many<T> items(T... items) {
        return stream(Arrays.stream(items));
    }

    @Override
    public <T> Many<T> stream(Stream<T> items) {
        return new ManyFromStream<>(items, new ManyState());
    }

    @Override
    public <T> Many<T> completion(CompletableFuture<T> completableFuture) {
        return null;
    }

    @Override
    public <T> Many<T> emitter(ThrowingConsumer<ManyEmitter<T>> emitterConsumer) {
        return new ManyFromEmitter<>(emitterConsumer, new ManyState());
    }

    @Override
    public <T> Many<T> error(Throwable throwable) {
        return null;
    }
}

