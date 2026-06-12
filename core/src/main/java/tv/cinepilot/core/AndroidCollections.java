package tv.cinepilot.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AndroidCollections {
    private AndroidCollections() {
    }

    public static <T> List<T> emptyList() {
        return Collections.emptyList();
    }

    public static <T> List<T> singletonList(T value) {
        return Collections.singletonList(value);
    }

    public static <T> List<T> listCopy(Collection<? extends T> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    public static <T> List<T> prepend(T first, List<? extends T> rest) {
        if (first == null) {
            return listCopy(rest);
        }
        ArrayList<T> result = new ArrayList<>();
        result.add(first);
        if (rest != null && !rest.isEmpty()) result.addAll(rest);
        return Collections.unmodifiableList(result);
    }

    public static <K, V> Map<K, V> emptyMap() {
        return Collections.emptyMap();
    }

    public static <K, V> Map<K, V> mapCopy(Map<? extends K, ? extends V> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
