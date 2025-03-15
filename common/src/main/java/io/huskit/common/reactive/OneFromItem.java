package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.SneakyThrows;

record OneFromItem<T>(T item) implements One<T> {

    @Override
    @SneakyThrows
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        consumer.accept(item);
    }
}
