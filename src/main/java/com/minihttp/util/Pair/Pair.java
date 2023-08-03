package com.minihttp.util.Pair;

import java.util.AbstractMap;
import java.util.Map;

public class Pair<K, V> extends AbstractMap.SimpleImmutableEntry<K, V> {

    public Pair(K key, V value) {
        super(key, value);
    }

    public Pair(Map.Entry<? extends K, ? extends V> entry) {
        super(entry);
    }
}