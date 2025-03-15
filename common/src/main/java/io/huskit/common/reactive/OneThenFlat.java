package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;

import java.util.function.Supplier;

record OneThenFlat<T>(One<?> delegate, Supplier<One<? extends T>> supplier) implements One<T> {

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        delegate.subscribe(ignore -> supplier.get().subscribe(consumer));
    }
}

