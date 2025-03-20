package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingFunction;

public interface ManySelect<T> {

    One<T> one(ThrowingFunction<? super T, One<Boolean>> function);
}
