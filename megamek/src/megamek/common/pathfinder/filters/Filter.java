/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.pathfinder.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a function that allows removing unwanted objects from a collection.
 */
public abstract class Filter<T> {
    /**
     * Returns filtered collection by removing those objects that fail {@link #shouldStay} test.
     *
     * @param collection collection to be filtered
     *
     * @return filtered collection
     */
    public Collection<T> doFilter(Collection<T> collection) {
        List<T> filteredMoves = new ArrayList<>();
        for (T e : collection) {
            if (shouldStay(e)) {
                filteredMoves.add(e);
            }
        }
        return filteredMoves;
    }

    /**
     * Tests if the object should stay in the collection.
     *
     * @param object tested object
     *
     * @return true if the object should stay in the collection
     */
    public abstract boolean shouldStay(T object);
}
