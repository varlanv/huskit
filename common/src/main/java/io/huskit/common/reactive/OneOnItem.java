package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class OneOnItem<T> implements One<T> {

    One<T> delegate;
    ThrowingConsumer<? super T> onItemConsumer;

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        delegate.subscribe(t -> {
            onItemConsumer.accept(t);
            consumer.accept(t);
        });
    }
}
