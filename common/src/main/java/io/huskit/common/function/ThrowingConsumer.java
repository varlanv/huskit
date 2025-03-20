package io.huskit.common.function;

import io.huskit.common.Sneaky;
import lombok.SneakyThrows;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> {

    void accept(T t) throws Exception;

    @SneakyThrows
    default Consumer<T> toUnchecked() {
        return t -> {
            try {
                accept(t);
            } catch (Exception e) {
                throw Sneaky.rethrow(e);
            }
        };
    }

    static <T> ThrowingConsumer<T> noop() {
        return t -> {
        };
    }
}
