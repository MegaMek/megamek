/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common;

import java.util.BitSet;
import java.util.Objects;

/**
 * Represents a set of flags that can be used to represent the type and
 * special properties of the equipment.
 * @author Luana Coppio
 */
public class EquipmentBitSet {

    private final BitSet bitSet;

    /**
     * Default constructor.
     */
    public EquipmentBitSet() {
        // This value is currently a bit above the double of what we need, but it's a power of 2, and it's a good
        // starting point since it will give a lot of runaway for new types of equipments, ammo and weapons to be added
        // Whenever we surpass this number, the bitset will increase on its own as needed, so its more of a performance
        // matter than a limitation.
        this.bitSet = new BitSet(512);
    }

    /**
     * Copy constructor.
     * @param other the EquipmentBitSet to copy
     */
    public EquipmentBitSet(EquipmentBitSet other) {
        this.bitSet = (BitSet) other.bitSet.clone();
    }

    /**
     * Returns true if the flag is set in the EquipmentBitSet.
     * @param flag the flag to check
     * @return true if the flag is set in the EquipmentBitSet
     */
    public boolean get(EquipmentFlag flag) {
        return bitSet.get(flag.getFlagIndex());
    }

    public boolean contains(EquipmentBitSet other) {
        var checker = new EquipmentBitSet(this);
        checker.bitSet.and(other.bitSet);
        return checker.equals(other);
    }

    /**
     * Clears the flag in the EquipmentBitSet.
     * @param flag the flag to clear
     */
    public void clear(EquipmentFlag flag) {
        bitSet.clear(flag.getFlagIndex());
    }

    /**
     * Clears all flags in the EquipmentBitSet.
     */
    public void clear() {
        bitSet.clear();
    }

    /**
     * Sets the flag in the EquipmentBitSet.
     * @param flag the flag to set
     */
    public void set(EquipmentFlag flag) {
        bitSet.set(flag.getFlagIndex());
    }


    /**
     * Returns a copy of this EquipmentBitSet with the flag set.
     * @param flag the flag to set
     * @return a copy of this EquipmentBitSet with the flag set
     */
    public EquipmentBitSet or(EquipmentFlag flag) {
        var newBitSet = new EquipmentBitSet(this);
        newBitSet.set(flag);
        return newBitSet;
    }

    /**
     * Returns a copy of this EquipmentBitSet with the flag cleared.
     * @param flag the flag to clear
     * @return a copy of this EquipmentBitSet with the flag cleared
     */
    public EquipmentBitSet andNot(EquipmentFlag flag) {
        var newBitSet = new EquipmentBitSet(this);
        newBitSet.clear(flag);
        return newBitSet;
    }

    /**
     * Returns a new empty EquipmentBitSet and the flag set if it is set in this EquipmentBitSet.
     * Example:
     *  EquipmentBitSet a = new EquipmentBitSet();
     *  a.set(F_HEAT_SINK);
     *  a.set(F_DOUBLE_HEATSINK);
     *  a.and(F_HEAT_SINK) // EquipmentBitSet with only F_HEAT_SINK set if it was originally set
     *  a.has(F_HEAT_SINK); // true
     *  a.has(F_DOUBLE_HEATSINK); // false
     * @param flag the flag to check
     * @return a new empty EquipmentBitSet and the flag set if it is set in this EquipmentBitSet
     */
    public EquipmentBitSet and(EquipmentFlag flag) {
        var newBitSet = new EquipmentBitSet();
        if (this.get(flag)) {
            newBitSet.set(flag);
        }
        return newBitSet;
    }

    @Override
    public String toString() {
        return "EntityBitSet{" +
            "bitSet=" + bitSet +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof EquipmentBitSet that) {
            return Objects.equals(bitSet, that.bitSet);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bitSet);
    }
}
