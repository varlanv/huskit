package io.huskit.common.reactive;

public interface ManyEmitter<T> {

    ManyEmitter<T> emit(T value);

    void complete();

    void fail(Throwable throwable);
}
