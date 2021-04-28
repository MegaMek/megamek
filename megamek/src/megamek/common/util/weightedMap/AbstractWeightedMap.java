/*
 * Copyright (C) 2021 - The MegaMek Team. All Rights Reserved
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
package megamek.common.util.weightedMap;

import megamek.common.annotations.Nullable;

import java.util.TreeMap;

public abstract class AbstractWeightedMap<K extends Number, T> extends TreeMap<K, T> {
    //region Variable Declarations
    private static final long serialVersionUID = 6745329554873562471L;
    //endregion Variable Declarations

    /**
     * @param weight the weight to put the item at
     * @param value  the value of the item
     * @return the return value of {@link TreeMap#put}, or null if the value is not added to the map
     */
    public abstract @Nullable T add(final K weight, final T value);

    /**
     * @return a random item from the weighted map, or null if it is empty
     */
    public abstract @Nullable T randomItem();
}
