package io.huskit.common.reactive;

import io.huskit.common.concurrent.FinishFuture;
import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;
import io.huskit.common.function.ThrowingRunnable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface One<T> {

    void subscribe(ThrowingConsumer<? super T> consumer);

    default One<T> onItem(ThrowingConsumer<? super T> consumer) {
        return new OneOnItem<>(this, consumer);
    }

    default One<T> onError(ThrowingConsumer<Throwable> consumer) {
        return new OneOnError<>(this, consumer);
    }

    default T block() {
        return toFuture().join();
    }

    default T block(Duration timeout) {
        return FinishFuture.finish(toFuture(), timeout);
    }

    default CompletableFuture<T> toFuture() {
        var future = new CompletableFuture<T>();
        onItem(future::complete).onError(future::completeExceptionally)
            .subscribe(ThrowingConsumer.noop());
        return future;
    }

    @SuppressWarnings("DataFlowIssue")
    default One<Void> mapToNothing() {
        return new OneFromTransform<>(this, t -> null);
    }

    default <R> One<R> then(Supplier<? extends R> supplier) {
        return new OneThen<>(this, supplier);
    }

    default <R> One<R> thenFlat(One<? extends R> one) {
        return new OneThenFlat<>(this, one);
    }

    default <R> Many<R> thenFlatMany(Many<? extends R> many) {
        return new OneThenFlatMany<>(this, many, new ManyState());
    }

    default <R> One<R> map(ThrowingFunction<? super T, ? extends R> mapper) {
        return new OneFromTransform<>(this, mapper);
    }

    default <R> One<R> flatMap(ThrowingFunction<? super T, One<? extends R>> mapper) {
        return new OneFromFlatTransform<>(this, mapper);
    }

    default <R> Many<R> flatMapMany(ThrowingFunction<? super T, ? extends Many<? extends R>> mapper) {
        return new ManyFromMappedOne<>(this, mapper, new ManyState());
    }

    default One<T> runOnComplete(ThrowingRunnable runnable) {
        return new OneWithRunOneComplete<>(this, runnable);
    }

    static OneFrom from() {
        return DfOneFrom.INSTANCE;
    }
}
