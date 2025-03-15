package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.SneakyThrows;

record OneFromError<T>(Throwable throwable) implements One<T> {

    @Override
    @SneakyThrows
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        throw throwable;
    }
}
