/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class counts elements
 *
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
        } else {
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
