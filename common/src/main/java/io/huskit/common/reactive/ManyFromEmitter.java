package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class ManyFromEmitter<T> implements Many<T> {

    ThrowingConsumer<ManyEmitter<T>> emitterConsumer;
    @Getter
    ManyState state;

    @Override
    @SneakyThrows
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        emitterConsumer.accept(new ManyEmitter<>() {

            @Override
            @SneakyThrows
            public ManyEmitter<T> emit(T value) {
                if (!state.isCanceled()) {
                    consumer.accept(value);
//                    if (!state.isCanceled()) {
//                        subscribe(consumer);
//                    }
                }
                return this;
            }

            @Override
            public void complete() {
                state.cancel();
            }

            @Override
            @SneakyThrows
            public void fail(Throwable throwable) {
                throw throwable;
            }
        });
    }
}
