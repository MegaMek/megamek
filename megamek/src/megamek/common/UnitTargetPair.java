/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import megamek.common.annotations.Nullable;

import java.util.Objects;

/**
 * This class is a record of pair of a (InGameObject) unit and a (Targetable) target.
 * Note that two such pairs are equal when their contents count as equal which is when the IDs of
 * unit and target are equal.
 * This object itself is immutable although the underlying objects are not.
 */
public final class UnitTargetPair {

    private final InGameObject unit;
    private final Targetable target;

    public UnitTargetPair(InGameObject unit, Targetable target) {
        this.unit = unit;
        this.target = target;
    }

    public InGameObject getUnit() {
        return unit;
    }

    public Targetable getTarget() {
        return target;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        } else if ((null == o) || (getClass() != o.getClass())) {
            return false;
        } else {
            final UnitTargetPair other = (UnitTargetPair) o;
            return Objects.equals(unit, other.unit) && Objects.equals(target, other.target);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, target);
    }
}
