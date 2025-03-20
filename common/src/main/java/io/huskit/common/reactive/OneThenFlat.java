package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class OneThenFlat<T, R> implements OperatorOne<T, R> {

    @Getter
    One<T> delegate;
    One<? extends R> one;

    @Override
    public void subscribe(ThrowingConsumer<? super R> consumer) {
        delegate.subscribe(ignore -> {
            one.subscribe(consumer);
        });
    }
}

