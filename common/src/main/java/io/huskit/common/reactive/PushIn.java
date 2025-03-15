package io.huskit.common.reactive;

public interface PushIn<T, R> {

    T request();

    PushOut<R> response();

    static <T, R> PushIn<T, R> of(T request, PushOut<R> response) {
        return new PushIn<>() {

            @Override
            public T request() {
                return request;
            }

            @Override
            public PushOut<R> response() {
                return response;
            }
        };
    }
}
