package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface ManyFrom {

    <T> Many<T> item(T item);

    <T> Many<T> items(T... items);

    <T> Many<T> stream(Stream<T> items);

    <T> Many<T> completion(CompletableFuture<T> completableFuture);

    <T> Many<T> emitter(ThrowingConsumer<ManyEmitter<T>> emitterConsumer);

    <T> Many<T> error(Throwable throwable);
}