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

import megamek.common.modifiers.EquipmentModifier;

import java.util.List;

/**
 * This interface is implemented by equipment, parts, units or other things that may have modifications such as a to-hit modifier or a heat
 * modifier.
 *
 * @see EquipmentModifier
 */
public interface Modifiable {

    /**
     * Returns all modifiers of this equipment or unit.
     *
     * This method must be overridden by implementing classes to provide a non-null (albeit often empty) return value. The returned list
     * must be the list kept by the object, in other words it must not be unmodifiable nor a copy, as some default methods of this interface
     * access that list.
     *
     * @see EquipmentModifier
     */
    List<EquipmentModifier> getModifiers();

    /**
     * Adds the given modifier to this equipment or unit (even if it is already present).
     *
     * This method usually needs no override.
     *
     * @param modifier The modifier to add
     */
    default void addEquipmentModifier(EquipmentModifier modifier) {
        getModifiers().add(modifier);
    }

    /**
     * Removes the given modifier from this equipment or unit. Does nothing if the modifier is not present.
     *
     * This method usually needs no override.
     *
     * @param modifier The modifier to remove
     */
    default void removeEquipmentModifier(EquipmentModifier modifier) {
        getModifiers().remove(modifier);
    }

    /**
     * Removes all modifiers from this equipment or unit.
     *
     * This method usually needs no override.
     */
    default void clearEquipmentModifiers() {
        getModifiers().clear();
    }

    /**
     * Returns true when there is a modifier present for this unit or equipment, false otherwise.
     *
     * This method usually needs no override.
     */
    default boolean isModified() {
        return !getModifiers().isEmpty();
    }
}
