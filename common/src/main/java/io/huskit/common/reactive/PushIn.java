package io.huskit.common.reactive;

public interface PushIn<T, R, U> {

    T request();

    PushOut<R, U> response();

    static <T, R, U> PushIn<T, R, U> of(T request, PushOut<R, U> response) {
        return new PushIn<>() {

            @Override
            public T request() {
                return request;
            }

            @Override
            public PushOut<R, U> response() {
                return response;
            }
        };
    }
}
