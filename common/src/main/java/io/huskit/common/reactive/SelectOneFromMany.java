package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingFunction;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SelectOneFromMany<T> implements One<T> {

    Many<T> delegate;
    ThrowingFunction<? super T, One<Boolean>> function;

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        delegate.subscribe(value -> {
            function.apply(value).subscribe(state -> {
                if (state) {
                    consumer.accept(value);
                }
            });
        });
    }
}
