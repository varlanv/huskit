package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;

import java.util.concurrent.CompletableFuture;

public interface Many<T> {

    void subscribe(ThrowingConsumer<? super T> consumer);

    default Many<T> selectWhile(ThrowingFunction<? extends T, One<Boolean>> function) {
        return new ManySelectWhile<>(this, function);
    }

    interface ManyFrom {

        <T> Many<T> item(T item);

        <T> Many<T> completion(CompletableFuture<T> completableFuture);

        <T> Many<T> emitter(ThrowingConsumer<OneEmitter<T>> emitterConsumer);

        <T> Many<T> error(Throwable throwable);
    }
}
