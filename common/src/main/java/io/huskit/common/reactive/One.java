package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;

import java.util.concurrent.CompletableFuture;

public interface One<T> {

    void subscribe(ThrowingConsumer<? super T> consumer);

    default CompletableFuture<T> toFuture() {
        var future = new CompletableFuture<T>();
        subscribe(t -> {
            try {
                future.complete(t);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    default <R> One<R> map(ThrowingFunction<? super T, ? extends R> mapper) {
        return new MappedOne<>(this, mapper);
    }

    default <R> One<R> flatMap(ThrowingFunction<? super T, One<? extends R>> mapper) {
        return new FlatMappedOne<>(this, mapper);
    }

    static OneFrom from() {
        return DfOneFrom.INSTANCE;
    }

    interface OneFrom {

        <T> One<T> item(T item);

        <T> One<T> completion(CompletableFuture<T> completableFuture);
    }
}
