package io.huskit.common.collection;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@UtilityClass
public class HtCollections {

    @SuppressWarnings("unchecked")
    public static <T> List<T> add(List<T> list, T... elements) {
        if (list.isEmpty()) {
            return Arrays.asList(elements);
        } else if (elements.length == 0) {
            return list;
        } else {
            var newList = new ArrayList<T>(list.size() + elements.length);
            newList.addAll(list);
            newList.addAll(Arrays.asList(elements));
            return newList;
        }
    }

    public static <T> List<T> add(T t1, T t2, List<T> elements) {
        if (elements.isEmpty()) {
            return List.of(t1, t2);
        } else {
            var newList = new ArrayList<T>(elements.size() + 2);
            newList.add(t1);
            newList.add(t2);
            newList.addAll(elements);
            return newList;
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T getFromMap(String key, Map<String, Object> map) {
        var val = map.get(key);
        if (val == null) {
            throw new IllegalStateException(String.format("Could not find key [%s] in container map", key));
        }
        return (T) val;
    }

    public static <K, V> void putOrAdd(Map<K, List<V>> map, K key, V value) {
        var list = map.get(key);
        if (list != null) {
            list.add(value);
        } else {
            var newList = new ArrayList<V>();
            newList.add(value);
            map.put(key, newList);
        }
    }

    public static List<String> toStringList(Iterable<?> iterable) {
        var list = new ArrayList<String>();
        for (var o : iterable) {
            list.add(o.toString());
        }
        return list;
    }
}
