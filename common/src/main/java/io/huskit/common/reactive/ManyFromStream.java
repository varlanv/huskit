package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@RequiredArgsConstructor
final class ManyFromStream<T> implements Many<T> {

    Stream<T> items;
    @Getter
    ManyState state;

    @Override
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        if (!state.isCanceled()) {
            items.forEach(consumer.toUnchecked());
            state.cancel();
        }
    }
}
