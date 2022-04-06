/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.codeUtilities;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class ObjectUtility {

    /**
     * The method is returns the same as a call to the following code:
     * <code>T result = (getFirst() != null) ? getFirst() : getSecond();</code>
     * with the major difference that getFirst() and getSecond() get evaluated exactly once.
     * <p>
     * This means that it doesn't matter if getFirst() is relatively expensive to evaluate or has
     * side effects. It also means that getSecond() gets evaluated <i>regardless</i> if it is
     * needed or not. Since Java guarantees the order of evaluation for arguments to be the same as
     * the order in which they appear (JSR 15.7.4), this makes it more suitable for re-playable
     * procedural generation and similar method calls with side effects.
     *
     * @return the first argument if it's not <i>null</i>, otherwise the second argument
     */
    public static @Nullable <T> T nonNull(final @Nullable T first, final @Nullable T second) {
        return (first == null) ? second : first;
    }

    /**
     * @return the first non-<i>null</i> argument, else <i>null</i> if all are <i>null</i>
     * @see #nonNull(T, T)
     */
    @SafeVarargs
    public static <T> T nonNull(final @Nullable T first, final @Nullable T second,
                                final T ... others) {
        if (first != null) {
            return first;
        } else if (second != null) {
            return second;
        }

        T result = others[0];
        int index = 1;
        while ((result == null) && (index < others.length)) {
            result = others[index++];
        }
        return result;
    }

    public static <T> int compareNullable(final @Nullable T a, final @Nullable T b,
                                          final Comparator<? super T> comparator) {
        if (a == b) { // Strict comparison is desired, to handle both null too
            return 0;
        } else if (a == null) {
            return 1;
        } else if (b == null) {
            return -1;
        } else {
            return comparator.compare(a, b);
        }
    }

    /**
     * Get a random element out of a collection, with equal probability.
     * <p>
     * This is the same as calling the following code, only plays nicely with all collections
     * (including ones like Set which don't implement RandomAccess) and deals gracefully with empty
     * collections.
     * <p>
     * <code>collection.get(Compute.randomInt(collection.size());</code>
     *
     * @return a random item from the collection. Returns <code>null</code> if the collection
     * itself is null or empty. It can also return <code>null</code> if the collection contains
     * <code>null</code> items.
     */
    public static @Nullable <T> T getRandomItem(final @Nullable Collection<? extends T> collection) {
        if ((collection == null) || collection.isEmpty()) {
            return null;
        }
        final int index = Compute.randomInt(collection.size());
        final Iterator<? extends T> iterator = collection.iterator();
        for (int i = 0; i < index; ++ i) {
            iterator.next();
        }
        return iterator.next();
    }

    /**
     * Get a random element out of a list, with equal probability.
     * <p>
     * This is the same as calling the following code, only deals gracefully with empty lists.
     * <p>
     * <code>list.get(Compute.randomInt(list.size());</code>
     *
     * @return a random item from the list. This will be <code>null</code> if the list itself is
     * null or empty, and can be <code>null</code> if the list contains <code>null</code> items.
     */
    public static @Nullable <T> T getRandomItem(final @Nullable List<? extends T> list) {
        return ((list == null) || list.isEmpty()) ? null : list.get(Compute.randomInt(list.size()));
    }
}
