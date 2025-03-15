package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.SneakyThrows;

record OneFromEmitter<T>(ThrowingConsumer<OneEmitter<T>> emitterConsumer) implements One<T> {

    @Override
    @SneakyThrows
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        emitterConsumer.accept(new OneEmitter<T>() {

            @Override
            @SneakyThrows
            public void complete(T value) {
                consumer.accept(value);
            }

            @Override
            @SneakyThrows
            public void fail(Throwable throwable) {
                throw throwable;
            }
        });
    }
}
