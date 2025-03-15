package io.huskit.common.reactive;

interface OperatorOne<T, R> extends One<R> {

    One<T> delegate();
}
