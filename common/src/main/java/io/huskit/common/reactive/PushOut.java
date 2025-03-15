package io.huskit.common.reactive;

import io.huskit.common.Mutable;

import java.util.Optional;
import java.util.function.Function;

public interface PushOut<T, R> {

    Optional<T> value();

    Optional<T> push(R data);

    default boolean isReady() {
        return value().isPresent();
    }

    static <T, R> PushOut<T, R> ready(T value) {
        return new PushOut<>() {

            @Override
            public Optional<T> value() {
                return Optional.of(value);
            }

            @Override
            public Optional<T> push(R data) {
                return Optional.of(value);
            }
        };
    }

    static <T, R> PushOut<T, R> fake(Function<R, Optional<T>> action) {
        return new PushOut<>() {

            Mutable<T> value = Mutable.of();

            @Override
            public Optional<T> value() {
                return value.maybe();
            }

            @Override
            public Optional<T> push(R data) {
                return value.maybe()
                    .or(
                        () -> {
                            var val = action.apply(data);
                            val.ifPresent(value::set);
                            return val;
                        }
                    );
            }
        };
    }
}
