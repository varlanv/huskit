package io.huskit.common.reactive;

import io.huskit.common.Mutable;

import java.util.Optional;

public class PushOnce<T> implements PushOut<T, T> {

    Mutable<T> value = Mutable.of();

    @Override
    public Optional<T> value() {
        return value.maybe();
    }

    @Override
    public Optional<T> push(T data) {
        value.set(data);
        return Optional.of(data);
    }
}
