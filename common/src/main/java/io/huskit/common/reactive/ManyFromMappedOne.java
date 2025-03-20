package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class ManyFromMappedOne<T, R> implements Many<R> {

    One<? extends T> delegate;
    ThrowingFunction<? super T, ? extends Many<? extends R>> mapper;
    @Getter
    ManyState state;

    @Override
    public void subscribe(ThrowingConsumer<? super R> consumer) {
        delegate.subscribe(t -> {
            if (!state.isCanceled()) {
                var many = mapper.apply(t);
                many.subscribe(consumer);
            }
        });
    }
}
