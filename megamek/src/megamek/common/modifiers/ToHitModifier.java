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

import megamek.common.TargetRollModifier;

public class ToHitModifier extends AbstractEquipmentModifier {

    private final int toHitModification;

    /**
     * Creates a heat modifier that adds the given deltaHeat value to the weapon's own heat generation. DeltaHeat can be less than 0, but
     * the final heat value of the weapon is capped to never be less than 0.
     */
    public ToHitModifier(int toHitModification, Reason reason) {
        super(reason);
        this.toHitModification = toHitModification;
    }

    public TargetRollModifier getToHitModifier() {
        return new TargetRollModifier(toHitModification, (reason() == Reason.PARTIAL_REPAIR) ? "partial repair" : "salvage quality");
    }

    /**
     * @return The to hit modifier of this modifier with a leading "+" if it is positive, i.e. "+2" or "-1" or "0".
     */
    public String formattedToHitModifier() {
        return formattedModifier(toHitModification);
    }
}
