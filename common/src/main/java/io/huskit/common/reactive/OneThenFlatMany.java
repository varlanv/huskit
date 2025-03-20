package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class OneThenFlatMany<T> implements Many<T> {

    One<?> one;
    Many<? extends T> delegate;
    @Getter
    ManyState state;

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        one.subscribe(ignore -> {
            if (!state.isCanceled()) {
                delegate.subscribe(consumer);
            }
        });
    }
}
