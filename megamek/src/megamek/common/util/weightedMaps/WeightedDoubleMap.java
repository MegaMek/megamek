/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.util.weightedMaps;

import megamek.common.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Constructs a table of values each with a double weight that makes them more or less likely to be
 * selected at random
 *
 * @param <T> The values in the table
 */
public class WeightedDoubleMap<T> extends AbstractWeightedMap<Double, T> {
    //region Variable Declarations
    private static final long serialVersionUID = -2040584639476035520L;
    //endregion Variable Declarations

    @Override
    public @Nullable T add(final Double weight, final T value) {
        return (weight > 0.0d) ? put(isEmpty() ? weight : lastKey() + weight, value) : null;
    }

    @Override
    public @Nullable T randomItem() {
        return isEmpty() ? null : randomItem(ThreadLocalRandom.current().nextDouble(0.0d, lastKey()));
    }
}
