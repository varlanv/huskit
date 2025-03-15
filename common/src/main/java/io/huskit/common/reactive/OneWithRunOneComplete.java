package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingRunnable;

record OneWithRunOneComplete<T>(One<T> delegate, ThrowingRunnable runnable) implements One<T> {

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        delegate.subscribe(t -> {
            try {
                consumer.accept(t);
            } finally {
                runnable.run();
            }
        });
    }
}
