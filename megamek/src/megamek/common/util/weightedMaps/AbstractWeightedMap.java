/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util.weightedMaps;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import megamek.common.annotations.Nullable;

public abstract class AbstractWeightedMap<K extends Number, T> extends TreeMap<K, T> {
    //region Variable Declarations
    private static final long serialVersionUID = 6745329554873562471L;
    //endregion Variable Declarations

    /**
     * @param weight the weight to put the item at
     * @param value  the value of the item
     *
     * @return the return value of {@link TreeMap#put}, or null if the value is not added to the map
     */
    public abstract @Nullable T add(final K weight, final T value);

    /**
     * @return a random item from the weighted map, or null if it is empty
     */
    public abstract @Nullable T randomItem();

    /**
     * @param key the key of the item to get (used for simplification and unit testing reasons)
     *
     * @return the item from the weighted map, or null if it does not exist
     */
    protected @Nullable T randomItem(final K key) {
        final Map.Entry<K, T> item = ceilingEntry(key);
        return (item == null) ? null : item.getValue();
    }

    /**
     * @return a random item from the weighted map boxed in an Optional, empty if the map is empty
     */
    public Optional<T> randomOptionalItem() {
        return Optional.ofNullable(randomItem());
    }
}
