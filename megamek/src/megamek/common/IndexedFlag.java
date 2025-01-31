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


/**
 * Interface for entity flags, used by {@link EquipmentBitSet}.
 * Allows for easy flag manipulation with enums that implement this interface.
 * Also provides a default method to convert a single flag to a bitset so
 * multiple flags can be composed into an {@link EquipmentBitSet}.
 * @author Luana Coppio
 */
public interface IndexedFlag {
    /**
     * Returns the index of the flag in the bitset
     * - Future improvements, return ordinal instead of arbitrary index
     * @return the index of the flag in the bitset
     */
    int getFlagIndex();

    /**
     * Converts this flag to an {@link EquipmentBitSet}.
     * @return an {@link EquipmentBitSet} with this flag set
     */
    default EquipmentBitSet asEquipmentBitSet() {
        return new EquipmentBitSet().or(this);
    }
}
