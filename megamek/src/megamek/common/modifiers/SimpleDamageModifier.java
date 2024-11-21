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
package megamek.common.modifiers;

import java.util.function.Function;

public class SimpleDamageModifier implements EquipmentModifier {

    private final Function<Integer, Integer> damageModification;

    /**
     * Creates a heat modifier that adds the given deltaHeat value to the weapon's own heat generation. DeltaHeat can be less than 0, but
     * the final heat value of the weapon is capped to never be less than 0.
     *
     * @param deltaDamage The heat value to add to the weapon's heat generation
     */
    public SimpleDamageModifier(int deltaDamage) {
        this(heat -> heat + deltaDamage);
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
     * @param damageModification The function to apply to the weapon's heat value
     */
    public SimpleDamageModifier(Function<Integer, Integer> damageModification) {
        this.damageModification = damageModification;
    }

    public int getModifiedDamage(int originalHeat) {
        return Math.max(0, damageModification.apply(originalHeat));
    }
}
