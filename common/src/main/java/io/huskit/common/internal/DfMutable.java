package io.huskit.common.internal;

import io.huskit.common.Mutable;
import io.huskit.common.function.*;
import lombok.*;
import lombok.experimental.NonFinal;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
@ToString(of = "value")
@EqualsAndHashCode(of = "value")
public class DfMutable<T> implements Mutable<T> {

    @Nullable
    @NonFinal
    private T value;

    @Override
    public void set(T value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public boolean isPresent() {
        return value != null;
    }

    @Override
    @SneakyThrows
    public boolean check(ThrowingPredicate<T> predicate) {
        var val = value;
        return val != null && predicate.test(val);
    }

    @Override
    public Optional<T> maybe() {
        return Optional.ofNullable(value);
    }

    @Override
    public T or(T other) {
        var value = this.value;
        return value != null ? value : Objects.requireNonNull(other, "other");
    }

    @Override
    @SneakyThrows
    public T or(ThrowingSupplier<T> supplier) {
        var value = this.value;
        return value != null ? value : Objects.requireNonNull(supplier.get(), "supplier");
    }

    @Override
    @SneakyThrows
    public <R> R mapOr(ThrowingFunction<T, R> mapper, ThrowingSupplier<R> other) {
        var value = this.value;
        return value != null ? Objects.requireNonNull(mapper.apply(value), "mapper") : Objects.requireNonNull(other.get(), "other");
    }

    @Override
    @SneakyThrows
    public void ifPresent(ThrowingConsumer<T> consumer) {
        var value = this.value;
        if (value != null) {
            consumer.accept(value);
        }
    }

    @Override
    @SneakyThrows
    public void ifPresentOrElse(ThrowingConsumer<T> consumer, ThrowingRunnable runnable) {
        var value = this.value;
        if (value != null) {
            consumer.accept(value);
        } else {
            runnable.run();
        }
    }

    @Override
    public T require() {
        var value = this.value;
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }
}
