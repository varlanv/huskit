package io.huskit.common.reactive;

import io.huskit.common.function.ThrowingConsumer;

import java.util.ArrayList;
import java.util.List;

public interface Many<T> {

    void subscribe(ThrowingConsumer<? super T> consumer);

    ManyState state();

    static ManyFrom from() {
        return DfManyFrom.INSTANCE;
    }

    default ManySelect<T> select() {
        return new DfManySelect<>(this);
    }

    default List<T> list() {
        var list = new ArrayList<T>();
        subscribe(list::add);
        return list;
    }
}
