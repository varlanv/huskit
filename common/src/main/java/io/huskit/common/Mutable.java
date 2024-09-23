package io.huskit.common;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingPredicate;

import java.util.Optional;

public interface Mutable<T> {

    void set(T value);

    boolean isPresent();

    boolean check(ThrowingPredicate<T> predicate);

    Optional<T> maybe();

    T or(T other);

    void ifPresent(ThrowingConsumer<T> consumer);

    T require();

    default boolean isEmpty() {
        return !isPresent();
    }
}