package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;

record ManyFromMappedOne<T, R>(One<? extends T> delegate,
                               ThrowingFunction<? super T, ? extends Many<? extends R>> mapper) implements Many<R> {

    @Override
    public void subscribe(ThrowingConsumer<? super R> consumer) {
        delegate.subscribe(t -> {
            var many = mapper.apply(t);
            many.subscribe(consumer);
        });
    }
}
