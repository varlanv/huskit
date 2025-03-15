package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;
import lombok.SneakyThrows;

record MappedOne<T, R>(One<T> delegate, ThrowingFunction<? super T, ? extends R> mapper) implements One<R> {

    @Override
    @SneakyThrows
    public void subscribe(ThrowingConsumer<? super R> consumer) {
        delegate.subscribe(t -> consumer.accept(mapper.apply(t)));
    }
}
