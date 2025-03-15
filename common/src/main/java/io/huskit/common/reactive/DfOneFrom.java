package io.huskit.common.reactive;

import java.util.concurrent.CompletableFuture;

class DfOneFrom implements One.OneFrom {

    static final DfOneFrom INSTANCE = new DfOneFrom();

    @Override
    public <T> One<T> item(T item) {
        return new OneFromItem<>(item);
    }

    @Override
    public <T> One<T> completion(CompletableFuture<T> completableFuture) {
        return new OneFromCompletion<>(completableFuture);
    }
}
