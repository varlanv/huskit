package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;
import io.huskit.common.function.ThrowingRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface One<T> {

    void subscribe(ThrowingConsumer<? super T> consumer);

    default T block() {
        return toFuture().join();
    }

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

    @SuppressWarnings("DataFlowIssue")
    default One<Void> mapToNothing() {
        return new OneFromTransform<>(this, t -> null);
    }

    default <R> One<R> then(Supplier<? extends R> supplier) {
        return new OneThen<>(this, supplier);
    }

    default <R> One<R> thenFlat(Supplier<One<? extends R>> supplier) {
        return new OneThenFlat<>(this, supplier);
    }

    default <R> One<R> map(ThrowingFunction<? super T, ? extends R> mapper) {
        return new OneFromTransform<>(this, mapper);
    }

    default <R> One<R> flatMap(ThrowingFunction<? super T, One<? extends R>> mapper) {
        return new OneFromFlatTransform<>(this, mapper);
    }

    default <R> Many<R> flatMapMany(ThrowingFunction<? super T, ? extends Many<? extends R>> mapper) {
        return new ManyFromMappedOne<>(this, mapper);
    }

    default One<T> runOnComplete(ThrowingRunnable runnable) {
        return new OneWithRunOneComplete<>(this, runnable);
    }

    static OneFrom from() {
        return DfOneFrom.INSTANCE;
    }

    interface OneFrom {

        <T> One<T> empty();

        <T> One<T> item(T item);

        <T> One<T> completion(CompletableFuture<T> completableFuture);

        <T> One<T> emitter(ThrowingConsumer<OneEmitter<T>> emitterConsumer);

        <T> One<T> error(Throwable throwable);
    }
}
