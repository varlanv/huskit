package io.huskit.common.reactive;

public interface OneEmitter<T> {

    void complete(T value);

    void fail(Throwable throwable);
}
