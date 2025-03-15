package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
final class OneThenFlat<T, R> implements OperatorOne<T, R> {

    @Getter
    One<T> delegate;
    Supplier<One<? extends R>> supplier;

    @Override
    public void subscribe(ThrowingConsumer<? super R> consumer) {
        delegate.subscribe(ignore -> {
            var one = supplier.get();
            one.subscribe(consumer);
        });
    }
}

