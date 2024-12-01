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

/**
 * This is an EquipmentModifier that changes the heat damage of a weapon.
 *
 * Note that multiple such modifiers can be applied to a weapon; their effects stack.
 *
 * TODO: this currently doesn't work; it shows up in the unit display but does not take effect in the weapon handlers.
 */
public class DamageModifier extends AbstractEquipmentModifier {

    private final int deltaDamage;

    /**
     * Creates a heat modifier that adds the given deltaHeat value to the weapon's own heat generation. DeltaHeat can be less than 0, but
     * the final heat value of the weapon is capped to never be less than 0.
     *
     * @param deltaDamage The heat value to add to the weapon's damage
     */
    public DamageModifier(int deltaDamage, Reason reason) {
        super(reason);
        this.deltaDamage = deltaDamage;
    }

    public int getDeltaDamage() {
        return deltaDamage;
    }

    /**
     * @return The damage modifier with a leading "+" if it is positive, i.e. "+2" or "-1" or "0".
     */
    public String formattedDamageModifier() {
        return formattedModifier(deltaDamage);
    }
}
