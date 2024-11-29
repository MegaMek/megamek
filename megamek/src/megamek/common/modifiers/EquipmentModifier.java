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

import java.io.Serializable;

/**
 * This interface is a marker for modifiers that change equipment or unit behavior. An example is a modifier that changes the heat a weapon
 * generates. Such modifiers can be: MM Scenario effects; MHQ permanent weapon modifications, such as through quality, permanent damage or
 * partial repairs (currently handled differently for Entity); potentially even modding.
 *
 * These modifiers are applied to Mounteds in MM (and possibly MML, at least for MHQ->MML operations) and Parts in MHQ. When starting a game
 * from MHQ, Part modifiers must be applied to the Mounteds of the unit (if they aren't already).
 *
 * There seems no way to deal with all such modifiers in a singular way as some will likely have to be applied in the game manager, others
 * in MM common classes and at various points in the code. Therefore this interface does not specify a method that implements the
 * modification.
 */
public interface EquipmentModifier extends Serializable {

    /**
     * The reason (origin) of an equipment modifier can be used to show appropriate GUI info on the modifier
     */
    enum Reason {

        /**
         * Used when an item suffers from Partial Repair, CO pp.205-207
         */
        PARTIAL_REPAIR,

        /**
         * Used when an item has fixed pre-existing damage, e.g. from a MM scenario. This is similar to partial repairs in the sense that an
         * item is partially repaired because it has been previously damaged but the distinction can be used to produce a UI text that makes
         * more sense than "partial repair" when it is just a fixed effect.
         */
        DAMAGED,

        /**
         * Used when an item is Salvage Quality, CO p.215
         */
        SALVAGE_QUALITY,

        /**
         * Fallback value; ideally not used
         */
        UNKNOWN
    }

    /**
     * @return The reason (origin) of this modifier
     */
    Reason reason();
}
