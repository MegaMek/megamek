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

package megamek.common;

import java.util.BitSet;

public class EquipmentFlagBitSet extends BitSet {
    public EquipmentFlagBitSet() {
        super(EquipmentFlag.values().length);
    }

    public EquipmentFlagBitSet or(EquipmentFlag other) {
        // Simplification of the original code
        // since a flag can have only one value, this is equivalent
        // to setting the flag in the flags position
        set(other.getFlagPosition());
        return this;
    }

    public EquipmentFlagBitSet or(EquipmentFlagBitSet other) {
        or((BitSet) other);
        return this;
    }

    /**
     * Boolean AND operation, changes the current set to the result of the operation
     * with the other flag, example
     * EquipmentFlagSet (A) = 1100
     * EquipmentFlag(2) (B) = 0100
     * A & B = 1100 & 0100 = 0100
     * This operation is destructive and will change the current state of the set
     * @param other the other flag to perform the operation with
     * @return the bitset flags
     */
    public EquipmentFlagBitSet and(EquipmentFlag other) {
        // simplification of the original code
        // since a flag can have only one value, this is equivalent
        // to clearing the entire set and setting the flag
        // if it was set before
        if (get(other.getFlagPosition())) {
            clear();
            set(other.getFlagPosition());
        } else {
            clear();
        }
        return this;
    }

    public EquipmentFlagBitSet and(EquipmentFlagBitSet other) {
        and((BitSet) other);
        return this;
    }

    public EquipmentFlagBitSet andNot(EquipmentFlag other) {
        // simplification of the original code
        // andNot simply turns off a value, therefore this should be equivalent
        // to A & ~B
        clear(other.getFlagPosition());
        return this;
    }

    public EquipmentFlagBitSet andNot(EquipmentFlagBitSet other) {
        andNot((BitSet) other);
        return this;
    }

    public EquipmentFlagBitSet set(EquipmentFlag other) {
        // a simple way to set a flag
        set(other.getFlagPosition());
        return this;
    }

    public EquipmentFlagBitSet clear(EquipmentFlag other) {
        // a simple way to remove a flag
        clear(other.getFlagPosition());
        return this;
    }

    public boolean isSet(EquipmentFlag flag) {
        return get(flag.getFlagPosition());
    }

    public boolean contains(EquipmentFlag flag) {
        return get(flag.getFlagPosition());
    }

    public boolean intersects(EquipmentFlagBitSet other) {
        return intersects((BitSet) other);
    }
}
