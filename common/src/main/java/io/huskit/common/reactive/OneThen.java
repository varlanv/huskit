package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;

import java.util.function.Supplier;

record OneThen<T>(One<?> delegate, Supplier<? extends T> supplier) implements One<T> {

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        delegate.subscribe(ignore -> consumer.accept(supplier.get()));
    }
}
