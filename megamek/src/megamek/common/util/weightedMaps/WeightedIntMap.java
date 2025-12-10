/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;

/**
 * Constructs a table of values each with an int weight that makes them more or less likely to be selected at random
 *
 * @param <T> The values in the table
 */
public class WeightedIntMap<T> extends AbstractWeightedMap<Integer, T> {
    //region Variable Declarations
    @Serial
    private static final long serialVersionUID = -568712793616821291L;
    //endregion Variable Declarations

    @Override
    public @Nullable T add(final Integer weight, final T value) {
        return (weight > 0) ? put(isEmpty() ? weight : lastKey() + weight, value) : null;
    }

    @Override
    public @Nullable T randomItem() {
        return isEmpty() ? null : randomItem(Compute.randomInt(lastKey()) + 1);
    }
}
