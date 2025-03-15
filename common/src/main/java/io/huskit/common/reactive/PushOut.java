package io.huskit.common.reactive;

import io.huskit.common.Mutable;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Function;

public interface PushOut<T> {

    Optional<T> value();

    Optional<T> push(ByteBuffer byteBuffer);

    default boolean isReady() {
        return value().isPresent();
    }

    static PushOut<?> ready() {
        return new PushOut<>() {

            @Override
            public Optional<Object> value() {
                return Optional.of(true);
            }

            @Override
            public Optional<Object> push(ByteBuffer byteBuffer) {
                return Optional.of(true);
            }
        };
    }

    static <T> PushOut<T> fake(Function<ByteBuffer, Optional<T>> action) {
        return new PushOut<>() {

            Mutable<T> value = Mutable.of();

            @Override
            public Optional<T> value() {
                return value.maybe();
            }

            @Override
            public Optional<T> push(ByteBuffer byteBuffer) {
                return value.maybe()
                    .or(
                        () -> {
                            var val = action.apply(byteBuffer);
                            val.ifPresent(value::set);
                            return val;
                        }
                    );
            }
        };
    }
}
