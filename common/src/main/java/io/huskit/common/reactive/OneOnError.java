package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class OneOnError<T> implements One<T> {

    One<T> delegate;
    ThrowingConsumer<Throwable> onErrorConsumer;

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        delegate.subscribe(t -> {
            try {
                consumer.accept(t);
            } catch (Throwable ex) {
                onErrorConsumer.accept(ex);
            }
        });
    }
}
