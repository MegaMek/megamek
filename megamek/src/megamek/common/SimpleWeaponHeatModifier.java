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

import java.util.function.Function;

/**
 * This is an EquipmentModifier that changes the heat generation of a weapon (WeaponMounted). This can be a simple addition or subtraction,
 * but also a more complicated method.
 *
 * Note that multiple such heat modifiers can be applied to a weapon. Their effects stack by being applied one after the other.
 */
public class SimpleWeaponHeatModifier implements EquipmentModifier {

    private final Function<Integer, Integer> heatModification;

    /**
     * Creates a heat modifier that adds the given deltaHeat value to the weapon's own heat generation. DeltaHeat can be less than 0, but
     * the final heat value of the weapon is capped to never be less than 0.
     *
     * @param deltaHeat The heat value to add to the weapon's heat generation
     */
    public SimpleWeaponHeatModifier(int deltaHeat) {
        this(heat -> heat + deltaHeat);
    }

    /**
     * Creates a heat modifier that performs the given function on the weapon's own heat generation value. Note that the final heat value of
     * the weapon is capped to never be less than 0.
     *
     * A modifier that increases weapon heat by 20% can be created like this:
     * <pre>{@code
     * new SimpleWeaponHeatModifier(heat -> (int) (1.2 * heat));
     * }</pre>
     *
     * @param heatModification The function to apply to the weapon's heat value
     */
    public SimpleWeaponHeatModifier(Function<Integer, Integer> heatModification) {
        this.heatModification = heatModification;
    }

    public int getModifiedHeat(int originalHeat) {
        return Math.max(0, heatModification.apply(originalHeat));
    }
}
