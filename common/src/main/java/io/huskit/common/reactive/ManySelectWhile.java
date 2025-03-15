package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;

record ManySelectWhile<T>(Many<T> delegate, ThrowingFunction<? extends T, One<Boolean>> function) implements Many<T> {

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
    }
}
