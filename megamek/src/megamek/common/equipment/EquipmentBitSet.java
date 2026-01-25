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

package megamek.common.equipment;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * Represents a set of flags that can be used to represent the type and special properties of the equipment.
 *
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
     *
     * @param other the EquipmentBitSet to copy
     */
    public EquipmentBitSet(EquipmentBitSet other) {
        this.bitSet = (BitSet) other.bitSet.clone();
    }

    /**
     * Returns true if the flag is set in the EquipmentBitSet.
     *
     * @param flag the flag to check
     *
     * @return true if the flag is set in the EquipmentBitSet
     */
    public boolean get(EquipmentFlag flag) {
        return bitSet.get(flag.ordinal());
    }

    public boolean contains(EquipmentBitSet other) {
        if (other == null) {return false;}
        BitSet oBits = other.bitSet;
        // Iterate over the bits set in 'other'
        for (int i = oBits.nextSetBit(0); i >= 0; i = oBits.nextSetBit(i + 1)) {
            // If 'this' is missing a bit that 'other' has, return false
            if (!this.bitSet.get(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean intersects(EquipmentBitSet other) {
        if (other == null) {return false;}
        return this.bitSet.intersects(other.bitSet);
    }

    /**
     * Clears the flag in the EquipmentBitSet.
     *
     * @param flag the flag to clear
     */
    public void clear(EquipmentFlag flag) {
        bitSet.clear(flag.ordinal());
    }

    /**
     * Clears all flags in the EquipmentBitSet.
     */
    public void clear() {
        bitSet.clear();
    }

    /**
     * Sets the flag in the EquipmentBitSet.
     *
     * @param flag the flag to set
     */
    public void set(EquipmentFlag flag) {
        bitSet.set(flag.ordinal());
    }

    /**
     * Returns a copy of this EquipmentBitSet with the specified flags set.
     *
     * @param flags the flags to set
     *
     * @return a copy of this EquipmentBitSet with the flags set
     */
    public EquipmentBitSet or(EquipmentFlag... flags) {
        var newBitSet = new EquipmentBitSet(this);
        for (EquipmentFlag flag : flags) {
            newBitSet.set(flag);
        }
        return newBitSet;
    }

    /**
     * Returns a copy of this EquipmentBitSet with the flag cleared.
     *
     * @param flag the flag to clear
     *
     * @return a copy of this EquipmentBitSet with the flag cleared
     */
    public EquipmentBitSet andNot(EquipmentFlag flag) {
        var newBitSet = new EquipmentBitSet(this);
        newBitSet.clear(flag);
        return newBitSet;
    }

    /**
     * Returns a new empty EquipmentBitSet and the flags set if it is set in this EquipmentBitSet.
     * Example:
     *  EquipmentBitSet a = new EquipmentBitSet();
     *  a.set(F_HEAT_SINK);
     *  a.set(F_DOUBLE_HEATSINK);
     *  a.and(F_HEAT_SINK) // EquipmentBitSet with only F_HEAT_SINK set if it was originally set
     *  a.has(F_HEAT_SINK); // true
     *  a.has(F_DOUBLE_HEATSINK); // false
     * @param flags the flags to check
     *
     * @return a new empty EquipmentBitSet and the flag set if it is set in this EquipmentBitSet
     */
    public EquipmentBitSet and(EquipmentFlag... flags) {
        var newBitSet = new EquipmentBitSet();
        for (EquipmentFlag flag : flags) {
            if (this.get(flag)) {
                newBitSet.set(flag);
            }
        }
        return newBitSet;
    }

    /**
     * Returns a list of string names for all flags that are set in this EquipmentBitSet.
     * @param flagEnum the enum class to check against (e.g., MiscTypeFlag.class, WeaponTypeFlag.class)
     * @return list of flag names that are set in this bitset
     */
    public <T extends Enum<T> & EquipmentFlag> List<String> getSetFlagNames(Class<T> flagEnum) {
        List<String> setFlags = new ArrayList<>();
        T[] enumConstants = flagEnum.getEnumConstants();
        if (enumConstants == null) {
            // The provided class is not an enum type; return empty list
            return setFlags;
        }

        // Only iterate through the bits that are actually set
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            // Check if this bit index corresponds to a valid enum ordinal
            if (i < enumConstants.length) {
                setFlags.add(enumConstants[i].name());
            }
            // Handle potential overflow (though very unlikely with 512-bit capacity)
            if (i == Integer.MAX_VALUE) {
                break;
            }
        }

        return setFlags;
    }

    /**
     * Returns an array of string names for all flags that are set in this EquipmentBitSet.
     * @param flagEnum the enum class to check against (e.g., MiscTypeFlag.class, WeaponTypeFlag.class)
     * @return array of flag names that are set in this bitset
     */
    public <T extends Enum<T> & EquipmentFlag> String[] getSetFlagNamesAsArray(Class<T> flagEnum) {
        List<String> flagNames = getSetFlagNames(flagEnum);
        return flagNames.toArray(new String[0]);
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
