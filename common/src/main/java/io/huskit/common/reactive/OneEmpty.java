package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;

final class OneEmpty<T> implements One<T> {

    static final OneEmpty<?> INSTANCE = new OneEmpty<>();

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public One<Void> mapToNothing() {
        return (One<Void>) INSTANCE;
    }
}
