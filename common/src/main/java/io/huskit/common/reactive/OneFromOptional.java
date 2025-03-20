package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;
import io.huskit.common.function.ThrowingSupplier;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Optional;

@RequiredArgsConstructor
public class OneFromOptional<T> implements One<T> {

    ThrowingSupplier<Optional<T>> supplier;

    @Override
    @SneakyThrows
    public void subscribe(ThrowingConsumer<? super T> consumer) {
        var value = supplier.get();
        value.ifPresent(consumer.toUnchecked());
    }
}
