package io.huskit.common.reactive;

import io.huskit.common.Volatile;

public final class ManyState {

    Volatile<Boolean> isCanceled = Volatile.of(false);

    public boolean isCanceled() {
        return isCanceled.require();
    }

    void cancel() {
        isCanceled.set(true);
    }
}
