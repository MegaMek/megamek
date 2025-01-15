/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class counts elements
 * @param <T>
 */
public class Counter<T> implements Collection<T> {

    private final Map<T, Long> map;

    public Counter() {
        this.map = new HashMap<>();
    }

    public Counter(List<T> initialValue) {
        this.map = initialValue.stream().collect(Collectors.groupingBy(s -> s,
            Collectors.counting()));
    }

    public T top() {
        return map.entrySet().stream()
            .max(Comparator.comparingLong(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public T bottom() {
        return map.entrySet().stream()
            .min(Comparator.comparingLong(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return map.keySet().toArray(a);
    }

    @Override
    public boolean add(T t) {
        if (map.containsKey(t)) {
            map.put(t, map.get(t) + 1);
        } else {
            map.put(t, 1L);
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Long) {
            return false;
        }
        else {
            var ret = map.remove(o);
            return ret != null;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T t : c) {
            add(t);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        for (Object o : c) {
            remove(o);
        }
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        for (T t : map.keySet()) {
            if (!c.contains(t)) {
                remove(t);
            }
        }
        return true;
    }

    @Override
    public void clear() {
        map.clear();
    }
}
