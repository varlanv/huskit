package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class OneFromTransform<T, R> implements OperatorOne<T, R> {

    @Getter
    One<T> delegate;
    ThrowingFunction<? super T, ? extends R> mapper;

    @Override
    public void subscribe(ThrowingConsumer<? super R> consumer) {
        delegate.subscribe(t -> consumer.accept(mapper.apply(t)));
    }
}
