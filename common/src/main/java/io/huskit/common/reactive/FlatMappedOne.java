package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;
import lombok.SneakyThrows;

record FlatMappedOne<T, R>(One<T> delegate,
                           ThrowingFunction<? super T, One<? extends R>> mapper) implements One<R> {

    @Override
    @SneakyThrows
    public void subscribe(ThrowingConsumer<? super R> consumer) {
        delegate.subscribe(value -> {
            One<? extends R> one = mapper.apply(value);
            one.subscribe(consumer);
        });
    }
}
