package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingFunction;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class DfManySelect<T> implements ManySelect<T> {

    Many<T> delegate;

    @Override
    public One<T> one(ThrowingFunction<? super T, One<Boolean>> function) {
        return new SelectOneFromMany<>(delegate, function);
    }
}
